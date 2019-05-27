package org.openlumify.web.routes.dashboard;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.webster.ParameterizedHandler;
import org.openlumify.webster.annotations.Handle;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.model.workspace.Dashboard;
import org.openlumify.core.model.workspace.WorkspaceRepository;
import org.openlumify.core.user.User;
import org.openlumify.core.util.ClientApiConverter;
import org.openlumify.web.clientapi.model.ClientApiDashboards;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;

import java.util.Collection;

@Singleton
public class DashboardAll implements ParameterizedHandler {
    private final WorkspaceRepository workspaceRepository;

    @Inject
    public DashboardAll(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    @Handle
    public ClientApiDashboards handle(
            @ActiveWorkspaceId String workspaceId,
            User user
    ) throws Exception {
        Collection<Dashboard> dashboards = workspaceRepository.findAllDashboardsForWorkspace(workspaceId, user);
        if (dashboards == null) {
            throw new OpenLumifyResourceNotFoundException("Could not find dashboards for workspace " + workspaceId);
        }
        return ClientApiConverter.toClientApiDashboards(dashboards);
    }
}
