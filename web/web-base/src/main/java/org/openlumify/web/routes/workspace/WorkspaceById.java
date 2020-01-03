package org.openlumify.web.routes.workspace;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.SecurityVertexiumException;
import org.openlumify.core.exception.OpenLumifyAccessDeniedException;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.model.workspace.Workspace;
import org.openlumify.core.model.workspace.WorkspaceRepository;
import org.openlumify.core.user.User;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;
import org.openlumify.web.clientapi.model.ClientApiWorkspace;

@Singleton
public class WorkspaceById implements ParameterizedHandler {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(WorkspaceById.class);
    private final WorkspaceRepository workspaceRepository;

    @Inject
    public WorkspaceById(
            final WorkspaceRepository workspaceRepository
    ) {
        this.workspaceRepository = workspaceRepository;
    }

    @Handle
    public ClientApiWorkspace handle(
            @Required(name = "workspaceId") String workspaceId,
            User user,
            Authorizations authorizations
    ) throws Exception {
        LOGGER.info("Attempting to retrieve workspace: %s", workspaceId);
        try {
            final Workspace workspace = workspaceRepository.findById(workspaceId, user);
            if (workspace == null) {
                throw new OpenLumifyResourceNotFoundException("Could not find workspace: " + workspaceId);
            } else {
                LOGGER.debug("Successfully found workspace");
                return workspaceRepository.toClientApi(workspace, user, authorizations);
            }
        } catch (SecurityVertexiumException ex) {
            throw new OpenLumifyAccessDeniedException("Could not get workspace " + workspaceId, user, workspaceId);
        }
    }
}
