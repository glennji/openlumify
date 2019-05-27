package org.openlumify.web.routes.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.webster.ParameterizedHandler;
import org.openlumify.webster.annotations.Handle;
import org.json.JSONObject;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;

import java.util.ResourceBundle;

@Singleton
public class Configuration implements ParameterizedHandler {
    private final org.openlumify.core.config.Configuration configuration;

    @Inject
    public Configuration(final org.openlumify.core.config.Configuration configuration) {
        this.configuration = configuration;
    }

    @Handle
    public JSONObject handle(
            ResourceBundle resourceBundle,
            @ActiveWorkspaceId(required = false) String workspaceId
    ) throws Exception {
        return this.configuration.toJSON(resourceBundle, workspaceId);
    }
}
