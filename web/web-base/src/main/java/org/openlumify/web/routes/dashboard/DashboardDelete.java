package org.openlumify.web.routes.dashboard;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.webster.ParameterizedHandler;
import org.openlumify.webster.annotations.Handle;
import org.openlumify.webster.annotations.Required;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.model.workspace.Dashboard;
import org.openlumify.core.model.workspace.WorkspaceRepository;
import org.openlumify.core.user.User;
import org.openlumify.web.OpenLumifyResponse;
import org.openlumify.web.clientapi.model.ClientApiSuccess;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;

@Singleton
public class DashboardDelete implements ParameterizedHandler {
    private final WorkspaceRepository workspaceRepository;

    @Inject
    public DashboardDelete(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    @Handle
    public ClientApiSuccess handle(
            @Required(name = "dashboardId") String dashboardId,
            @ActiveWorkspaceId String workspaceId,
            User user
    ) throws Exception {

        Dashboard dashboard = workspaceRepository.findDashboardById(workspaceId, dashboardId, user);
        if (dashboard == null) {
            throw new OpenLumifyResourceNotFoundException("Could not find dashboard with id " + dashboardId);
        }

        workspaceRepository.deleteDashboard(workspaceId, dashboardId, user);
        return OpenLumifyResponse.SUCCESS;
    }
}
