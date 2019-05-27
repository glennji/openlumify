package org.openlumify.vertexium.model.user;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.json.JSONObject;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.vertexium.VertexBuilder;
import org.vertexium.mutation.ExistingElementMutation;
import org.vertexium.query.QueryResultsIterable;
import org.vertexium.util.ConvertingIterable;
import org.openlumify.core.config.Configuration;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.model.lock.LockRepository;
import org.openlumify.core.model.ontology.Concept;
import org.openlumify.core.model.ontology.OntologyRepository;
import org.openlumify.core.model.properties.OpenLumifyProperties;
import org.openlumify.core.model.user.*;
import org.openlumify.core.security.OpenLumifyVisibility;
import org.openlumify.core.trace.Traced;
import org.openlumify.core.user.SystemUser;
import org.openlumify.core.user.User;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.vertexium.util.IterableUtils.singleOrDefault;
import static org.openlumify.core.model.ontology.OntologyRepository.PUBLIC;

@Singleton
public class VertexiumUserRepository extends UserRepository {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(VertexiumUserRepository.class);
    private Graph graph;
    private String userConceptId;
    private org.vertexium.Authorizations authorizations;
    private final Cache<String, Vertex> userVertexCache = CacheBuilder.newBuilder()
            .expireAfterWrite(15, TimeUnit.SECONDS)
            .build();

    @Inject
    public VertexiumUserRepository(
            Configuration configuration,
            GraphAuthorizationRepository graphAuthorizationRepository,
            Graph graph,
            OntologyRepository ontologyRepository,
            UserSessionCounterRepository userSessionCounterRepository,
            LockRepository lockRepository,
            AuthorizationRepository authorizationRepository,
            PrivilegeRepository privilegeRepository
    ) {
        super(
                configuration,
                userSessionCounterRepository,
                lockRepository,
                authorizationRepository,
                privilegeRepository
        );
        this.graph = graph;

        graphAuthorizationRepository.addAuthorizationToGraph(VISIBILITY_STRING);
        graphAuthorizationRepository.addAuthorizationToGraph(OpenLumifyVisibility.SUPER_USER_VISIBILITY_STRING);

        Concept userConcept = ontologyRepository.getOrCreateConcept(null, USER_CONCEPT_IRI, "openlumifyUser", null, false, getSystemUser(), PUBLIC);
        userConceptId = userConcept.getIRI();

        Set<String> authorizationsSet = new HashSet<>();
        authorizationsSet.add(VISIBILITY_STRING);
        authorizationsSet.add(OpenLumifyVisibility.SUPER_USER_VISIBILITY_STRING);
        this.authorizations = graph.createAuthorizations(authorizationsSet);
    }

    private VertexiumUser createFromVertex(Vertex user) {
        if (user == null) {
            return null;
        }

        LOGGER.debug("Creating user from UserRow. username: %s", UserOpenLumifyProperties.USERNAME.getPropertyValue(user));
        return new VertexiumUser(user);
    }

    @Override
    public User findByUsername(String username) {
        username = formatUsername(username);
        Iterable<Vertex> vertices = graph.query(authorizations)
                .has(UserOpenLumifyProperties.USERNAME.getPropertyName(), username)
                .has(OpenLumifyProperties.CONCEPT_TYPE.getPropertyName(), userConceptId)
                .vertices();
        Vertex userVertex = singleOrDefault(vertices, null);
        if (userVertex == null) {
            return null;
        }
        userVertexCache.put(userVertex.getId(), userVertex);
        return createFromVertex(userVertex);
    }

    @Override
    public Iterable<User> find(int skip, int limit) {
        QueryResultsIterable<Vertex> userVertices = graph.query(authorizations)
                .has(OpenLumifyProperties.CONCEPT_TYPE.getPropertyName(), userConceptId)
                .skip(skip)
                .limit(limit)
                .vertices();
        return new ConvertingIterable<Vertex, User>(userVertices) {
            @Override
            protected User convert(Vertex vertex) {
                return createFromVertex(vertex);
            }
        };
    }

    @Override
    @Traced
    public User findById(String userId) {
        if (SystemUser.USER_ID.equals(userId)) {
            return getSystemUser();
        }
        return createFromVertex(findByIdUserVertex(userId));
    }

    @Traced
    public Vertex findByIdUserVertex(String userId) {
        Vertex userVertex = userVertexCache.getIfPresent(userId);
        if (userVertex != null) {
            return userVertex;
        }
        userVertex = graph.getVertex(userId, authorizations);
        if (userVertex != null) {
            userVertexCache.put(userId, userVertex);
        }
        return userVertex;
    }

    @Override
    protected User addUser(String username, String displayName, String emailAddress, String password) {
        username = formatUsername(username);
        displayName = displayName.trim();

        byte[] salt = UserPasswordUtil.getSalt();
        byte[] passwordHash = UserPasswordUtil.hashPassword(password, salt);

        String id = GRAPH_USER_ID_PREFIX + graph.getIdGenerator().nextId();
        VertexBuilder userBuilder = graph.prepareVertex(id, VISIBILITY.getVisibility());

        OpenLumifyProperties.CONCEPT_TYPE.setProperty(userBuilder, userConceptId, VISIBILITY.getVisibility());
        UserOpenLumifyProperties.USERNAME.setProperty(userBuilder, username, VISIBILITY.getVisibility());
        UserOpenLumifyProperties.DISPLAY_NAME.setProperty(userBuilder, displayName, VISIBILITY.getVisibility());
        UserOpenLumifyProperties.CREATE_DATE.setProperty(userBuilder, new Date(), VISIBILITY.getVisibility());
        UserOpenLumifyProperties.PASSWORD_SALT.setProperty(userBuilder, salt, VISIBILITY.getVisibility());
        UserOpenLumifyProperties.PASSWORD_HASH.setProperty(userBuilder, passwordHash, VISIBILITY.getVisibility());

        if (emailAddress != null) {
            UserOpenLumifyProperties.EMAIL_ADDRESS.setProperty(userBuilder, emailAddress, VISIBILITY.getVisibility());
        }

        User user = createFromVertex(userBuilder.save(this.authorizations));
        graph.flush();

        afterNewUserAdded(user);

        return user;
    }

    @Override
    public void setPassword(User user, byte[] salt, byte[] passwordHash) {
        Vertex userVertex = findByIdUserVertex(user.getUserId());
        UserOpenLumifyProperties.PASSWORD_SALT.setProperty(userVertex, salt, VISIBILITY.getVisibility(), authorizations);
        UserOpenLumifyProperties.PASSWORD_HASH.setProperty(
                userVertex,
                passwordHash,
                VISIBILITY.getVisibility(),
                authorizations
        );
        graph.flush();
    }

    @Override
    public boolean isPasswordValid(User user, String password) {
        try {
            Vertex userVertex = findByIdUserVertex(user.getUserId());
            return UserPasswordUtil.validatePassword(
                    password,
                    UserOpenLumifyProperties.PASSWORD_SALT.getPropertyValue(userVertex),
                    UserOpenLumifyProperties.PASSWORD_HASH.getPropertyValue(userVertex)
            );
        } catch (Exception ex) {
            throw new RuntimeException("error validating password", ex);
        }
    }

    @Override
    public void updateUser(User user, AuthorizationContext authorizationContext) {
        Vertex userVertex = findByIdUserVertex(user.getUserId());
        ExistingElementMutation<Vertex> m = userVertex.prepareMutation();

        Date currentLoginDate = UserOpenLumifyProperties.CURRENT_LOGIN_DATE.getPropertyValue(userVertex);
        if (currentLoginDate != null) {
            UserOpenLumifyProperties.PREVIOUS_LOGIN_DATE.setProperty(m, currentLoginDate, VISIBILITY.getVisibility());
        }

        String currentLoginRemoteAddr = UserOpenLumifyProperties.CURRENT_LOGIN_REMOTE_ADDR.getPropertyValue(userVertex);
        if (currentLoginRemoteAddr != null) {
            UserOpenLumifyProperties.PREVIOUS_LOGIN_REMOTE_ADDR.setProperty(
                    m,
                    currentLoginRemoteAddr,
                    VISIBILITY.getVisibility()
            );
        }

        UserOpenLumifyProperties.CURRENT_LOGIN_DATE.setProperty(m, new Date(), VISIBILITY.getVisibility());
        UserOpenLumifyProperties.CURRENT_LOGIN_REMOTE_ADDR.setProperty(
                m,
                authorizationContext.getRemoteAddr(),
                VISIBILITY.getVisibility()
        );

        int loginCount = UserOpenLumifyProperties.LOGIN_COUNT.getPropertyValue(userVertex, 0);
        UserOpenLumifyProperties.LOGIN_COUNT.setProperty(m, loginCount + 1, VISIBILITY.getVisibility());

        m.save(authorizations);
        graph.flush();

        getPrivilegeRepository().updateUser(user, authorizationContext);
        getAuthorizationRepository().updateUser(user, authorizationContext);
        fireUserLoginEvent(user, authorizationContext);
    }

    @Override
    public User setCurrentWorkspace(String userId, String workspaceId) {
        User user = findById(userId);
        checkNotNull(user, "Could not find user: " + userId);
        Vertex userVertex = findByIdUserVertex(user.getUserId());
        if (workspaceId == null) {
            UserOpenLumifyProperties.CURRENT_WORKSPACE.removeProperty(userVertex, authorizations);
        } else {
            UserOpenLumifyProperties.CURRENT_WORKSPACE.setProperty(
                    userVertex,
                    workspaceId,
                    VISIBILITY.getVisibility(),
                    authorizations
            );
        }
        graph.flush();
        return user;
    }

    @Override
    public String getCurrentWorkspaceId(String userId) {
        User user = findById(userId);
        checkNotNull(user, "Could not find user: " + userId);
        Vertex userVertex = findByIdUserVertex(user.getUserId());
        return UserOpenLumifyProperties.CURRENT_WORKSPACE.getPropertyValue(userVertex);
    }

    @Override
    public void setUiPreferences(User user, JSONObject preferences) {
        Vertex userVertex = findByIdUserVertex(user.getUserId());
        UserOpenLumifyProperties.UI_PREFERENCES.setProperty(
                userVertex,
                preferences,
                VISIBILITY.getVisibility(),
                authorizations
        );
        graph.flush();
    }

    @Override
    public void setDisplayName(User user, String displayName) {
        Vertex userVertex = findByIdUserVertex(user.getUserId());
        UserOpenLumifyProperties.DISPLAY_NAME.setProperty(
                userVertex,
                displayName,
                VISIBILITY.getVisibility(),
                authorizations
        );
        graph.flush();
    }

    @Override
    public void setEmailAddress(User user, String emailAddress) {
        Vertex userVertex = findByIdUserVertex(user.getUserId());
        UserOpenLumifyProperties.EMAIL_ADDRESS.setProperty(
                userVertex,
                emailAddress,
                VISIBILITY.getVisibility(),
                authorizations
        );
        graph.flush();
    }

    @Override
    protected void internalDelete(User user) {
        Vertex userVertex = findByIdUserVertex(user.getUserId());
        graph.softDeleteVertex(userVertex, authorizations);
        graph.flush();
    }

    @Override
    public User findByPasswordResetToken(String token) {
        QueryResultsIterable<Vertex> userVertices = graph.query(authorizations)
                .has(UserOpenLumifyProperties.PASSWORD_RESET_TOKEN.getPropertyName(), token)
                .has(OpenLumifyProperties.CONCEPT_TYPE.getPropertyName(), userConceptId)
                .vertices();
        Vertex user = singleOrDefault(userVertices, null);
        return createFromVertex(user);
    }

    @Override
    public void setPasswordResetTokenAndExpirationDate(User user, String token, Date expirationDate) {
        Vertex userVertex = findByIdUserVertex(user.getUserId());
        UserOpenLumifyProperties.PASSWORD_RESET_TOKEN.setProperty(
                userVertex,
                token,
                VISIBILITY.getVisibility(),
                authorizations
        );
        UserOpenLumifyProperties.PASSWORD_RESET_TOKEN_EXPIRATION_DATE.setProperty(
                userVertex,
                expirationDate,
                VISIBILITY.getVisibility(),
                authorizations
        );
        graph.flush();
    }

    @Override
    public void clearPasswordResetTokenAndExpirationDate(User user) {
        Vertex userVertex = findByIdUserVertex(user.getUserId());
        UserOpenLumifyProperties.PASSWORD_RESET_TOKEN.removeProperty(userVertex, authorizations);
        UserOpenLumifyProperties.PASSWORD_RESET_TOKEN_EXPIRATION_DATE.removeProperty(userVertex, authorizations);
        graph.flush();
    }

    @Override
    public void setPropertyOnUser(User user, String propertyName, Object value) {
        if (user instanceof SystemUser) {
            throw new OpenLumifyException("Cannot set properties on system user");
        }
        if (!value.equals(user.getCustomProperties().get(propertyName))) {
            Vertex userVertex = findByIdUserVertex(user.getUserId());
            userVertex.setProperty(propertyName, value, VISIBILITY.getVisibility(), authorizations);
            if (user instanceof VertexiumUser) {
                ((VertexiumUser) user).setProperty(propertyName, value);
            }
            graph.flush();
        }
    }

    public Vertex getUserVertex(String userId) {
        return findByIdUserVertex(userId);
    }
}
