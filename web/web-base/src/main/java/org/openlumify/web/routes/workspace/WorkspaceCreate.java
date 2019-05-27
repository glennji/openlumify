package org.openlumify.web.routes.workspace;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.vertexium.Authorizations;
import org.openlumify.core.model.lock.LockRepository;
import org.openlumify.core.model.user.AuthorizationRepository;
import org.openlumify.core.model.workQueue.WorkQueueRepository;
import org.openlumify.core.model.workspace.Workspace;
import org.openlumify.core.model.workspace.WorkspaceRepository;
import org.openlumify.core.user.User;
import org.openlumify.core.util.StreamUtil;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;
import org.openlumify.web.clientapi.model.ClientApiWorkspace;
import org.openlumify.webster.ParameterizedHandler;
import org.openlumify.webster.annotations.Handle;
import org.openlumify.webster.annotations.Optional;

@Singleton
public class WorkspaceCreate implements ParameterizedHandler {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(WorkspaceCreate.class);

    private final WorkspaceRepository workspaceRepository;
    private final WorkQueueRepository workQueueRepository;
    private final AuthorizationRepository authorizationRepository;
    private final LockRepository lockRepository;

    @Inject
    public WorkspaceCreate(
            final WorkspaceRepository workspaceRepository,
            final WorkQueueRepository workQueueRepository,
            AuthorizationRepository authorizationRepository,
            LockRepository lockRepository
    ) {
        this.workspaceRepository = workspaceRepository;
        this.workQueueRepository = workQueueRepository;
        this.authorizationRepository = authorizationRepository;
        this.lockRepository = lockRepository;
    }

    @Handle
    public ClientApiWorkspace handle(
            @Optional(name = "title") String title,
            User user
    ) throws Exception {
        String workspaceTitle = title == null ? workspaceRepository.getDefaultWorkspaceName(user) : title;
        String lockName = user.getUserId() + "|" + workspaceTitle;

        // We need to lock because whenever the last workspace is deleted all connected clients will try to 
        // create a new workspace, and locking here is easier than coordinating client side.
        return lockRepository.lock(lockName, () -> {
            Iterable<Workspace> workspaces = workspaceRepository.findAllForUser(user);
            java.util.Optional<Workspace> found = StreamUtil.stream(workspaces).filter(w -> {
                if (w.getDisplayTitle().equals(workspaceTitle)) {
                    String creatorUserId = workspaceRepository.getCreatorUserId(w.getWorkspaceId(), user);
                    if (user.getUserId().equals(creatorUserId)) {
                        return true;
                    }
                }
                return false;
            }).findFirst();

            Workspace foundOrCreated = null;
            boolean created = false;

            if (found.isPresent()) {
                foundOrCreated = found.get();
            } else {
                created = true;
                foundOrCreated = workspaceRepository.add(workspaceTitle, user);
                LOGGER.info("Created workspace: %s, title: %s", foundOrCreated.getWorkspaceId(), foundOrCreated.getDisplayTitle());
            }

            Authorizations authorizations = authorizationRepository.getGraphAuthorizations(user);
            ClientApiWorkspace clientApi = workspaceRepository.toClientApi(foundOrCreated, user, authorizations);

            if (created) {
                workQueueRepository.pushWorkspaceChange(clientApi, clientApi.getUsers(), user.getUserId(), null);
            }

            return clientApi;
        });
    }
}
