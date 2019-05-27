package org.openlumify.core.model.user;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.json.JSONObject;
import org.vertexium.TextIndexHint;
import org.openlumify.core.bootstrap.InjectHelper;
import org.openlumify.core.config.Configurable;
import org.openlumify.core.config.Configuration;
import org.openlumify.core.model.notification.ExpirationAge;
import org.openlumify.core.model.notification.UserNotification;
import org.openlumify.core.model.notification.UserNotificationRepository;
import org.openlumify.core.model.ontology.Concept;
import org.openlumify.core.model.ontology.OntologyPropertyDefinition;
import org.openlumify.core.model.ontology.OntologyRepository;
import org.openlumify.core.model.user.cli.PrivilegeRepositoryCliService;
import org.openlumify.core.model.user.cli.PrivilegeRepositoryWithCliSupport;
import org.openlumify.core.model.workQueue.WorkQueueRepository;
import org.openlumify.core.user.SystemUser;
import org.openlumify.core.user.User;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;
import org.openlumify.web.clientapi.model.Privilege;
import org.openlumify.web.clientapi.model.PropertyType;

import java.util.*;

import static org.openlumify.core.model.ontology.OntologyRepository.PUBLIC;

@Singleton
public class UserPropertyPrivilegeRepository extends PrivilegeRepositoryBase implements PrivilegeRepositoryWithCliSupport {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(UserPropertyPrivilegeRepository.class);
    public static final String PRIVILEGES_PROPERTY_IRI = "http://openlumify.org/user#privileges";
    public static final String CONFIGURATION_PREFIX = UserPropertyPrivilegeRepository.class.getName();
    private final ImmutableSet<String> defaultPrivileges;
    private final Configuration configuration;
    private final UserNotificationRepository userNotificationRepository;
    private final WorkQueueRepository workQueueRepository;
    private Collection<UserListener> userListeners;

    private static class Settings {
        @Configurable()
        public String defaultPrivileges;
    }

    @Inject
    public UserPropertyPrivilegeRepository(
            OntologyRepository ontologyRepository,
            Configuration configuration,
            UserNotificationRepository userNotificationRepository,
            WorkQueueRepository workQueueRepository
    ) {
        super(configuration);
        this.configuration = configuration;
        this.userNotificationRepository = userNotificationRepository;
        this.workQueueRepository = workQueueRepository;
        definePrivilegesProperty(ontologyRepository);

        Settings settings = new Settings();
        configuration.setConfigurables(settings, CONFIGURATION_PREFIX);
        this.defaultPrivileges = ImmutableSet.copyOf(Privilege.stringToPrivileges(settings.defaultPrivileges));
    }

    private void definePrivilegesProperty(OntologyRepository ontologyRepository) {
        List<Concept> concepts = new ArrayList<>();
        concepts.add(ontologyRepository.getConceptByIRI(UserRepository.USER_CONCEPT_IRI, PUBLIC));
        OntologyPropertyDefinition propertyDefinition = new OntologyPropertyDefinition(
                concepts,
                PRIVILEGES_PROPERTY_IRI,
                "Privileges",
                PropertyType.STRING
        );
        propertyDefinition.setUserVisible(false);
        propertyDefinition.setTextIndexHints(TextIndexHint.NONE);
        ontologyRepository.getOrCreateProperty(propertyDefinition, new SystemUser(), PUBLIC);
    }

    @Override
    public void updateUser(User user, AuthorizationContext authorizationContext) {
    }

    @Override
    public Set<String> getPrivileges(User user) {
        if (user instanceof SystemUser) {
            return Sets.newHashSet();
        }
        String privileges = (String) user.getProperty(PRIVILEGES_PROPERTY_IRI);
        if (privileges == null) {
            return new HashSet<>(defaultPrivileges);
        }
        return Privilege.stringToPrivileges(privileges);
    }

    public void setPrivileges(User user, Set<String> privileges, User authUser) {
        if (!privileges.equals(getPrivileges(user))) {
            String privilegesString = Privilege.toString(privileges);
            LOGGER.info(
                    "Setting privileges to '%s' on user '%s' by '%s'",
                    privilegesString,
                    user.getUsername(),
                    authUser.getUsername()
            );
            getUserRepository().setPropertyOnUser(user, PRIVILEGES_PROPERTY_IRI, privilegesString);
            sendNotificationToUserAboutPrivilegeChange(user, privileges, authUser);
            workQueueRepository.pushUserAccessChange(user);
            fireUserPrivilegesUpdatedEvent(user, privileges);
        }
    }

    private void sendNotificationToUserAboutPrivilegeChange(User user, Set<String> privileges, User authUser) {
        String title = "Privileges Changed";
        String message = "New Privileges: " + Joiner.on(", ").join(privileges);
        String actionEvent = null;
        JSONObject actionPayload = null;
        ExpirationAge expirationAge = null;
        UserNotification userNotification = userNotificationRepository.createNotification(
                user.getUserId(),
                title,
                message,
                actionEvent,
                actionPayload,
                expirationAge,
                authUser
        );
        workQueueRepository.pushUserNotification(userNotification);
    }

    private void fireUserPrivilegesUpdatedEvent(User user, Set<String> privileges) {
        for (UserListener userListener : getUserListeners()) {
            userListener.userPrivilegesUpdated(user, privileges);
        }
    }

    private Collection<UserListener> getUserListeners() {
        if (userListeners == null) {
            userListeners = InjectHelper.getInjectedServices(UserListener.class, configuration);
        }
        return userListeners;
    }

    @Override
    public PrivilegeRepositoryCliService getCliService() {
        return new UserPropertyPrivilegeRepositoryCliService(this);
    }

    public ImmutableSet<String> getDefaultPrivileges() {
        return defaultPrivileges;
    }
}
