package org.openlumify.vertexium.model.search;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.json.JSONObject;
import org.vertexium.*;
import org.openlumify.core.config.Configuration;
import org.openlumify.core.exception.OpenLumifyAccessDeniedException;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.model.graph.GraphRepository;
import org.openlumify.core.model.graph.GraphUpdateContext;
import org.openlumify.core.model.properties.types.PropertyMetadata;
import org.openlumify.core.model.search.SearchProperties;
import org.openlumify.core.model.search.SearchRepository;
import org.openlumify.core.model.user.AuthorizationRepository;
import org.openlumify.core.model.user.GraphAuthorizationRepository;
import org.openlumify.core.model.user.PrivilegeRepository;
import org.openlumify.core.model.user.UserRepository;
import org.openlumify.core.model.workQueue.Priority;
import org.openlumify.core.model.workspace.WorkspaceRepository;
import org.openlumify.core.security.OpenLumifyVisibility;
import org.openlumify.core.user.SystemUser;
import org.openlumify.core.user.User;
import org.openlumify.core.util.ClientApiConverter;
import org.openlumify.web.clientapi.model.ClientApiSearch;
import org.openlumify.web.clientapi.model.ClientApiSearchListResponse;
import org.openlumify.web.clientapi.model.Privilege;
import org.openlumify.web.clientapi.model.VisibilityJson;

import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.openlumify.core.util.StreamUtil.stream;

@Singleton
public class VertexiumSearchRepository extends SearchRepository {
    public static final String VISIBILITY_STRING = "search";
    public static final OpenLumifyVisibility VISIBILITY = new OpenLumifyVisibility(VISIBILITY_STRING);
    private static final String GLOBAL_SAVED_SEARCHES_ROOT_VERTEX_ID = "__openlumify_globalSavedSearchesRoot";
    private final Graph graph;
    private final GraphRepository graphRepository;
    private final UserRepository userRepository;
    private final AuthorizationRepository authorizationRepository;
    private final PrivilegeRepository privilegeRepository;
    private final WorkspaceRepository workspaceRepository;

    @Inject
    public VertexiumSearchRepository(
            Graph graph,
            GraphRepository graphRepository,
            UserRepository userRepository,
            Configuration configuration,
            GraphAuthorizationRepository graphAuthorizationRepository,
            AuthorizationRepository authorizationRepository,
            PrivilegeRepository privilegeRepository,
            WorkspaceRepository workspaceRepository
    ) {
        super(configuration);
        this.graph = graph;
        this.graphRepository = graphRepository;
        this.userRepository = userRepository;
        this.authorizationRepository = authorizationRepository;
        this.privilegeRepository = privilegeRepository;
        this.workspaceRepository = workspaceRepository;
        graphAuthorizationRepository.addAuthorizationToGraph(VISIBILITY_STRING);
    }

    @Override
    public String saveSearch(
            String id,
            String name,
            String url,
            JSONObject searchParameters,
            User user
    ) {
        checkNotNull(user, "User is required");
        Authorizations authorizations = authorizationRepository.getGraphAuthorizations(
                user,
                VISIBILITY_STRING,
                UserRepository.VISIBILITY_STRING
        );

        if (graph.doesVertexExist(id, authorizations)) {
            // switching from global to private
            if (isSearchGlobal(id, authorizations)) {
                if (privilegeRepository.hasPrivilege(user, Privilege.SEARCH_SAVE_GLOBAL)) {
                    deleteSearch(id, user);
                } else {
                    throw new OpenLumifyAccessDeniedException(
                            "User does not have the privilege to change a global search", user, id);
                }
            } else if (!isSearchPrivateToUser(id, user, authorizations)) {
                throw new OpenLumifyAccessDeniedException("User does not own this this search", user, id);
            }
        }

        try (GraphUpdateContext ctx = graphRepository.beginGraphUpdate(Priority.LOW, user, authorizations)) {
            ctx.setPushOnQueue(false);
            Vertex searchVertex = saveSearchVertex(ctx, id, name, url, searchParameters, user).get();

            Vertex userVertex = graph.getVertex(user.getUserId(), authorizations);
            checkNotNull(userVertex, "Could not find user vertex with id " + user.getUserId());
            String edgeId = userVertex.getId() + "_" + SearchProperties.HAS_SAVED_SEARCH + "_" + searchVertex.getId();
            ctx.getOrCreateEdgeAndUpdate(
                    edgeId,
                    userVertex.getId(),
                    searchVertex.getId(),
                    SearchProperties.HAS_SAVED_SEARCH,
                    VISIBILITY.getVisibility(),
                    elemCtx -> {
                    }
            );

            return searchVertex.getId();
        } catch (Exception ex) {
            throw new OpenLumifyException("Could not save private search", ex);
        }
    }

    @Override
    public String saveGlobalSearch(
            String id,
            String name,
            String url,
            JSONObject searchParameters,
            User user
    ) {
        if (!(user instanceof SystemUser) && !privilegeRepository.hasPrivilege(user, Privilege.SEARCH_SAVE_GLOBAL)) {
            throw new OpenLumifyAccessDeniedException(
                    "User does not have the privilege to save a global search", user, id);
        }

        Authorizations authorizations = authorizationRepository.getGraphAuthorizations(
                userRepository.getSystemUser(),
                VISIBILITY_STRING,
                UserRepository.VISIBILITY_STRING
        );

        // switching from private to global
        if (isSearchPrivateToUser(id, user, authorizations)) {
            deleteSearch(id, user);
        }

        try (GraphUpdateContext ctx = graphRepository.beginGraphUpdate(Priority.LOW, user, authorizations)) {
            ctx.setPushOnQueue(false);
            Vertex searchVertex = saveSearchVertex(ctx, id, name, url, searchParameters, user).get();

            String edgeId = String.format(
                    "%s_%s_%s",
                    GLOBAL_SAVED_SEARCHES_ROOT_VERTEX_ID, SearchProperties.HAS_SAVED_SEARCH, searchVertex.getId()
            );
            ctx.getOrCreateEdgeAndUpdate(
                    edgeId,
                    getGlobalSavedSearchesRootVertex().getId(),
                    searchVertex.getId(),
                    SearchProperties.HAS_SAVED_SEARCH,
                    VISIBILITY.getVisibility(),
                    elemCtx -> {
                    }
            );

            return searchVertex.getId();
        } catch (Exception ex) {
            throw new OpenLumifyException("Could not save global search", ex);
        }
    }

    private GraphUpdateContext.UpdateFuture<Vertex> saveSearchVertex(
            GraphUpdateContext ctx,
            String id,
            String name,
            String url,
            JSONObject searchParameters,
            User user
    ) {
        Visibility visibility = VISIBILITY.getVisibility();
        return ctx.getOrCreateVertexAndUpdate(id, visibility, elemCtx -> {
            PropertyMetadata metadata = new PropertyMetadata(user, new VisibilityJson(), visibility);
            if (elemCtx.isNewElement()) {
                elemCtx.updateBuiltInProperties(metadata);
            }
            elemCtx.setConceptType(SearchProperties.CONCEPT_TYPE_SAVED_SEARCH);
            SearchProperties.NAME.updateProperty(elemCtx, name != null ? name : "", metadata);
            SearchProperties.URL.updateProperty(elemCtx, url, metadata);
            SearchProperties.PARAMETERS.updateProperty(elemCtx, searchParameters, metadata);
        });
    }

    @Override
    public ClientApiSearchListResponse getSavedSearches(User user) {
        checkNotNull(user, "User is required");
        Authorizations authorizations = authorizationRepository.getGraphAuthorizations(
                user,
                VISIBILITY_STRING,
                UserRepository.VISIBILITY_STRING
        );

        ClientApiSearchListResponse result = new ClientApiSearchListResponse();
        Iterables.addAll(result.searches, getGlobalSavedSearches(authorizations));
        Iterables.addAll(result.searches, getUserSavedSearches(user, authorizations));
        return result;
    }

    private Iterable<ClientApiSearch> getUserSavedSearches(User user, Authorizations authorizations) {
        Vertex userVertex = graph.getVertex(user.getUserId(), authorizations);
        checkNotNull(userVertex, "Could not find user vertex with id " + user.getUserId());
        Iterable<Vertex> userSearchVertices = userVertex.getVertices(
                Direction.OUT,
                SearchProperties.HAS_SAVED_SEARCH,
                authorizations
        );
        return stream(userSearchVertices)
                .map(searchVertex -> toClientApiSearch(searchVertex, ClientApiSearch.Scope.User))
                .collect(Collectors.toList());
    }

    private Iterable<ClientApiSearch> getGlobalSavedSearches(Authorizations authorizations) {
        Vertex globalSavedSearchesRootVertex = getGlobalSavedSearchesRootVertex();
        Iterable<Vertex> globalSearchVertices = globalSavedSearchesRootVertex.getVertices(
                Direction.OUT,
                SearchProperties.HAS_SAVED_SEARCH,
                authorizations
        );
        return stream(globalSearchVertices)
                .map(searchVertex -> toClientApiSearch(searchVertex, ClientApiSearch.Scope.Global))
                .collect(Collectors.toList());
    }

    private Vertex getGlobalSavedSearchesRootVertex() {
        Authorizations authorizations = authorizationRepository.getGraphAuthorizations(
                userRepository.getSystemUser(),
                VISIBILITY_STRING
        );
        Vertex globalSavedSearchesRootVertex = graph.getVertex(GLOBAL_SAVED_SEARCHES_ROOT_VERTEX_ID, authorizations);
        if (globalSavedSearchesRootVertex == null) {
            globalSavedSearchesRootVertex = graph.prepareVertex(
                    GLOBAL_SAVED_SEARCHES_ROOT_VERTEX_ID,
                    new Visibility(VISIBILITY_STRING)
            )
                    .save(authorizations);
            graph.flush();
        }
        return globalSavedSearchesRootVertex;
    }

    private static ClientApiSearch toClientApiSearch(Vertex searchVertex) {
        return toClientApiSearch(searchVertex, null);
    }

    public static ClientApiSearch toClientApiSearch(Vertex searchVertex, ClientApiSearch.Scope scope) {
        ClientApiSearch result = new ClientApiSearch();
        result.id = searchVertex.getId();
        result.name = SearchProperties.NAME.getPropertyValue(searchVertex);
        result.url = SearchProperties.URL.getPropertyValue(searchVertex);
        result.scope = scope;
        result.parameters = ClientApiConverter.toClientApiValue(SearchProperties.PARAMETERS.getPropertyValue(
                searchVertex));
        return result;
    }

    @Override
    public ClientApiSearch getSavedSearch(String id, User user) {
        Authorizations authorizations = authorizationRepository.getGraphAuthorizations(
                user,
                VISIBILITY_STRING,
                UserRepository.VISIBILITY_STRING
        );
        Vertex searchVertex = graph.getVertex(id, authorizations);
        if (searchVertex == null) {
            return null;
        }
        return toClientApiSearch(searchVertex);
    }

    @Override
    public ClientApiSearch getSavedSearchOnWorkspace(String id, User user, String workspaceId) {
        Authorizations authorizations = authorizationRepository.getGraphAuthorizations(
                user,
                VISIBILITY_STRING,
                UserRepository.VISIBILITY_STRING
        );

        Vertex searchVertex = graph.getVertex(id, authorizations);
        if (searchVertex == null) {
            return null;
        }

        boolean isGlobalSearch = isSearchGlobal(id, authorizations);
        boolean hasWorkspaceAccess = workspaceId != null && workspaceRepository.hasReadPermissions(workspaceId, user);

        if (isGlobalSearch || isSearchPrivateToUser(id, user, authorizations)) {
            return toClientApiSearch(searchVertex);
        } else if (!isGlobalSearch && !hasWorkspaceAccess) {
            return null;
        } else {
            String workspaceCreatorId = workspaceRepository.getCreatorUserId(workspaceId, user);
            if (isSearchPrivateToUser(id, userRepository.findById(workspaceCreatorId), authorizations)) {
                return toClientApiSearch(searchVertex);
            }
            return null;
        }
    }

    @Override
    public void deleteSearch(final String id, User user) {
        checkNotNull(user, "User is required");
        Authorizations authorizations = authorizationRepository.getGraphAuthorizations(
                user,
                VISIBILITY_STRING,
                UserRepository.VISIBILITY_STRING
        );
        Vertex searchVertex = graph.getVertex(id, authorizations);
        checkNotNull(searchVertex, "Could not find search with id " + id);

        if (isSearchGlobal(id, authorizations)) {
            if (!privilegeRepository.hasPrivilege(user, Privilege.SEARCH_SAVE_GLOBAL)) {
                throw new OpenLumifyAccessDeniedException(
                        "User does not have the privilege to delete a global search", user, id);
            }
        } else if (!isSearchPrivateToUser(id, user, authorizations)) {
            throw new OpenLumifyAccessDeniedException("User does not own this this search", user, id);
        }

        graph.deleteVertex(searchVertex, authorizations);
        graph.flush();
    }

    @VisibleForTesting
    boolean isSearchGlobal(String id, Authorizations authorizations) {
        if (!graph.doesVertexExist(id, authorizations)) {
            return false;
        }
        Iterable<String> vertexIds = getGlobalSavedSearchesRootVertex().getVertexIds(
                Direction.OUT,
                SearchProperties.HAS_SAVED_SEARCH,
                authorizations
        );
        return stream(vertexIds).anyMatch(vertexId -> vertexId.equals(id));
    }

    @VisibleForTesting
    boolean isSearchPrivateToUser(String id, User user, Authorizations authorizations) {
        if (user instanceof SystemUser) {
            return false;
        }
        Vertex userVertex = graph.getVertex(user.getUserId(), authorizations);
        checkNotNull(userVertex, "Could not find user vertex with id " + user.getUserId());
        Iterable<String> vertexIds = userVertex.getVertexIds(
                Direction.OUT,
                SearchProperties.HAS_SAVED_SEARCH,
                authorizations
        );
        return stream(vertexIds).anyMatch(vertexId -> vertexId.equals(id));
    }
}
