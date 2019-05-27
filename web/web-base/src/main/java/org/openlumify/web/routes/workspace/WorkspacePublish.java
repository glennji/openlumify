package org.openlumify.web.routes.workspace;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.webster.ParameterizedHandler;
import org.openlumify.webster.annotations.Handle;
import org.openlumify.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.openlumify.core.model.workspace.WorkspaceRepository;
import org.openlumify.core.user.User;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;
import org.openlumify.web.clientapi.model.ClientApiPublishItem;
import org.openlumify.web.clientapi.model.ClientApiWorkspacePublishResponse;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;

@Singleton
public class WorkspacePublish implements ParameterizedHandler {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(WorkspacePublish.class);
    private final WorkspaceRepository workspaceRepository;

    @Inject
    public WorkspacePublish(final WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    @Handle
    public ClientApiWorkspacePublishResponse handle(
            @Required(name = "publishData") ClientApiPublishItem[] publishData,
            @ActiveWorkspaceId String workspaceId,
            User user,
            Authorizations authorizations
    ) throws Exception {
        LOGGER.debug("publishing:\n%s", Joiner.on("\n").join(publishData));
        ClientApiWorkspacePublishResponse workspacePublishResponse = workspaceRepository.publish(publishData, user, workspaceId, authorizations);

        LOGGER.debug("publishing results: %s", workspacePublishResponse);
        return workspacePublishResponse;
    }
}
