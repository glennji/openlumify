package org.openlumify.web.plugin.adminUserTools;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Required;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.model.user.UserRepository;
import org.openlumify.core.model.workspace.Workspace;
import org.openlumify.core.model.workspace.WorkspaceRepository;
import org.openlumify.core.user.User;
import org.openlumify.web.OpenLumifyResponse;
import org.openlumify.web.clientapi.model.ClientApiSuccess;
import org.openlumify.web.clientapi.model.WorkspaceAccess;

@Singleton
public class WorkspaceShareWithMe implements ParameterizedHandler {
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;

    @Inject
    public WorkspaceShareWithMe(
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository
    ) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
    }

    @Handle
    public ClientApiSuccess handle(
            @Required(name = "workspaceId") String workspaceId,
            @Required(name = "user-name") String userName,
            User me
    ) throws Exception {
        User user = userRepository.findByUsername(userName);
        if (user == null) {
            throw new OpenLumifyResourceNotFoundException("Could not find user: " + userName);
        }

        Workspace workspace = workspaceRepository.findById(workspaceId, user);
        if (workspace == null) {
            throw new OpenLumifyResourceNotFoundException("Could not find workspace: " + workspaceId);
        }

        workspaceRepository.updateUserOnWorkspace(workspace, me.getUserId(), WorkspaceAccess.WRITE, user);

        return OpenLumifyResponse.SUCCESS;
    }
}
