package org.openlumify.web.routes.dashboard;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Required;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.model.workspace.DashboardItem;
import org.openlumify.core.model.workspace.WorkspaceRepository;
import org.openlumify.core.user.User;
import org.openlumify.web.OpenLumifyResponse;
import org.openlumify.web.clientapi.model.ClientApiSuccess;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;

@Singleton
public class DashboardItemDelete implements ParameterizedHandler {
    private final WorkspaceRepository workspaceRepository;

    @Inject
    public DashboardItemDelete(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    @Handle
    public ClientApiSuccess handle(
            @Required(name = "dashboardItemId") String dashboardItemId,
            @ActiveWorkspaceId String workspaceId,
            User user
    ) throws Exception {
        DashboardItem dashboardItem = workspaceRepository.findDashboardItemById(workspaceId, dashboardItemId, user);
        if (dashboardItem == null) {
            throw new OpenLumifyResourceNotFoundException("Could not find dashboard item with id " + dashboardItemId);
        }

        workspaceRepository.deleteDashboardItem(workspaceId, dashboardItemId, user);
        return OpenLumifyResponse.SUCCESS;
    }
}
