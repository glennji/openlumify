package org.openlumify.web.routes.workspace;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Required;
import org.json.JSONObject;
import org.vertexium.Authorizations;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.model.notification.ExpirationAge;
import org.openlumify.core.model.notification.ExpirationAgeUnit;
import org.openlumify.core.model.notification.UserNotificationRepository;
import org.openlumify.core.model.workQueue.WorkQueueRepository;
import org.openlumify.core.model.workspace.Workspace;
import org.openlumify.core.model.workspace.WorkspaceRepository;
import org.openlumify.core.user.User;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;
import org.openlumify.web.clientapi.model.ClientApiWorkspace;
import org.openlumify.web.clientapi.model.ClientApiWorkspaceUpdateData;
import org.openlumify.web.clientapi.model.WorkspaceAccess;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;
import org.openlumify.web.parameterProviders.SourceGuid;

import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;

@Singleton
public class WorkspaceUpdate implements ParameterizedHandler {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(WorkspaceUpdate.class);
    private final WorkspaceRepository workspaceRepository;
    private final WorkQueueRepository workQueueRepository;
    private final UserNotificationRepository userNotificationRepository;

    @Inject
    public WorkspaceUpdate(
            final WorkspaceRepository workspaceRepository,
            final WorkQueueRepository workQueueRepository,
            final UserNotificationRepository userNotificationRepository
    ) {
        this.workspaceRepository = workspaceRepository;
        this.workQueueRepository = workQueueRepository;
        this.userNotificationRepository = userNotificationRepository;
    }

    @Handle
    public ClientApiWorkspace handle(
            @Required(name = "data") ClientApiWorkspaceUpdateData updateData,
            @ActiveWorkspaceId String workspaceId,
            @SourceGuid String sourceGuid,
            ResourceBundle resourceBundle,
            User user,
            Authorizations authorizations
    ) throws Exception {
        Workspace workspace = workspaceRepository.findById(workspaceId, user);
        if (workspace == null) {
            throw new OpenLumifyResourceNotFoundException("Could not find workspace: " + workspaceId);
        }

        if (updateData.getTitle() != null) {
            setTitle(workspace, updateData.getTitle(), user);
        }


        updateUsers(workspace, updateData.getUserUpdates(), resourceBundle, user);

        workspace = workspaceRepository.findById(workspaceId, user);
        ClientApiWorkspace clientApiWorkspaceAfterUpdateButBeforeDelete = workspaceRepository.toClientApi(
                workspace,
                user,
                authorizations
        );
        List<ClientApiWorkspace.User> previousUsers = clientApiWorkspaceAfterUpdateButBeforeDelete.getUsers();
        deleteUsers(workspace, updateData.getUserDeletes(), user);

        ClientApiWorkspace clientApiWorkspace = workspaceRepository.toClientApi(workspace, user, authorizations);

        workQueueRepository.pushWorkspaceChange(clientApiWorkspace, previousUsers, user.getUserId(), sourceGuid);

        return workspaceRepository.toClientApi(workspace, user, authorizations);
    }

    private void setTitle(Workspace workspace, String title, User authUser) {
        LOGGER.debug("setting title (%s): %s", workspace.getWorkspaceId(), title);
        workspaceRepository.setTitle(workspace, title, authUser);
    }

    private void deleteUsers(Workspace workspace, List<String> userDeletes, User authUser) {
        for (String userId : userDeletes) {
            LOGGER.debug("user delete (%s): %s", workspace.getWorkspaceId(), userId);
            workspaceRepository.deleteUserFromWorkspace(workspace, userId, authUser);
            workQueueRepository.pushWorkspaceDelete(workspace.getWorkspaceId(), userId);
        }
    }

    private void updateUsers(
            Workspace workspace,
            List<ClientApiWorkspaceUpdateData.UserUpdate> userUpdates,
            ResourceBundle resourceBundle,
            User authUser
    ) {
        for (ClientApiWorkspaceUpdateData.UserUpdate update : userUpdates) {
            LOGGER.debug("user update (%s): %s", workspace.getWorkspaceId(), update.toString());
            String userId = update.getUserId();
            WorkspaceAccess workspaceAccess = update.getAccess();
            WorkspaceRepository.UpdateUserOnWorkspaceResult updateUserOnWorkspaceResults
                    = workspaceRepository.updateUserOnWorkspace(workspace, userId, workspaceAccess, authUser);

            String title;
            String subtitle;
            switch (updateUserOnWorkspaceResults) {
                case UPDATE:
                    title = resourceBundle.getString("workspaces.notification.shareUpdated.title");
                    subtitle = resourceBundle.getString("workspaces.notification.shareUpdated.subtitle");
                    break;
                default:
                    title = resourceBundle.getString("workspaces.notification.shared.title");
                    subtitle = resourceBundle.getString("workspaces.notification.shared.subtitle");
            }
            String message = MessageFormat.format(subtitle, authUser.getDisplayName(), workspace.getDisplayTitle());
            JSONObject payload = new JSONObject();
            payload.put("workspaceId", workspace.getWorkspaceId());
            userNotificationRepository.createNotification(
                    userId,
                    title,
                    message,
                    "switchWorkspace",
                    payload,
                    new ExpirationAge(7, ExpirationAgeUnit.DAY),
                    authUser
            );
        }
    }

}
