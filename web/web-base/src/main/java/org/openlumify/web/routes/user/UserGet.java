package org.openlumify.web.routes.user;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.vertexium.Authorizations;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.model.user.AuthorizationRepository;
import org.openlumify.core.model.user.UserRepository;
import org.openlumify.core.model.user.UserSessionCounterRepository;
import org.openlumify.core.model.workspace.Workspace;
import org.openlumify.core.model.workspace.WorkspaceRepository;
import org.openlumify.core.user.User;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;
import org.openlumify.web.clientapi.model.ClientApiUser;
import org.openlumify.webster.ParameterizedHandler;
import org.openlumify.webster.annotations.Handle;
import org.openlumify.webster.annotations.Required;

@Singleton
public class UserGet implements ParameterizedHandler {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(UserGet.class);

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final AuthorizationRepository authorizationRepository;
    private final UserSessionCounterRepository userSessionCounterRepository;

    @Inject
    public UserGet(
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository,
            AuthorizationRepository authorizationRepository,
            UserSessionCounterRepository userSessionCounterRepository
    ) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.authorizationRepository = authorizationRepository;
        this.userSessionCounterRepository = userSessionCounterRepository;
    }

    @Handle
    public ClientApiUser handle(
            @Required(name = "user-name") String userName
    ) throws Exception {
        User user = userRepository.findByUsername(userName);
        if (user == null) {
            throw new OpenLumifyResourceNotFoundException("user not found");
        }

        Authorizations authorizations = authorizationRepository.getGraphAuthorizations(user);

        ClientApiUser clientApiUser = userRepository.toClientApiPrivate(user);

        addSessionCount(clientApiUser);

        Iterable<Workspace> workspaces = workspaceRepository.findAllForUser(user);
        for (Workspace workspace : workspaces) {
            clientApiUser.getWorkspaces().add(workspaceRepository.toClientApi(workspace, user, authorizations));
        }

        return clientApiUser;
    }

    private void addSessionCount(ClientApiUser user) {
        String id = user.getId();
        try {
            int count = userSessionCounterRepository.getSessionCount(id);
            user.setSessionCount(count);
        } catch (OpenLumifyException e) {
            LOGGER.error("Error getting session count for userId: %s", id, e);
        }
    }
}
