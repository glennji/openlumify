package org.openlumify.web.routes.dashboard;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Optional;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openlumify.core.model.workspace.WorkspaceRepository;
import org.openlumify.core.user.User;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;

@Singleton
public class DashboardUpdate implements ParameterizedHandler {
    private final WorkspaceRepository workspaceRepository;

    @Inject
    public DashboardUpdate(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    @Handle
    public JSONObject handle(
            @Optional(name = "dashboardId") String dashboardId,
            @Optional(name = "title") String title,
            @Optional(name = "items[]") String[] items,
            @ActiveWorkspaceId String workspaceId,
            User user
    ) throws Exception {
        JSONObject json = new JSONObject();
        dashboardId = workspaceRepository.addOrUpdateDashboard(workspaceId, dashboardId, title, user);
        json.put("id", dashboardId);
        if (items != null) {
            JSONArray itemIds = new JSONArray();
            for (String item : items) {
                JSONObject itemJson = new JSONObject(item);
                String itemTitle = itemJson.optString("title");
                String itemConfig = itemJson.optString("configuration");
                String itemExtension = itemJson.getString("extensionId");
                itemIds.put(workspaceRepository.addOrUpdateDashboardItem(workspaceId, dashboardId, null, itemTitle, itemConfig, itemExtension, user));
            }
            json.put("itemIds", itemIds);
        }
        // TODO: change this to a model object
        return json;
    }
}

