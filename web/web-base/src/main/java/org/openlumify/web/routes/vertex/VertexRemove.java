package org.openlumify.web.routes.vertex;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.openlumify.core.exception.OpenLumifyAccessDeniedException;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.model.workQueue.Priority;
import org.openlumify.core.model.workQueue.WorkQueueRepository;
import org.openlumify.core.model.workspace.WorkspaceHelper;
import org.openlumify.core.security.ACLProvider;
import org.openlumify.core.user.User;
import org.openlumify.core.util.SandboxStatusUtil;
import org.openlumify.web.OpenLumifyResponse;
import org.openlumify.web.clientapi.model.ClientApiSuccess;
import org.openlumify.web.clientapi.model.SandboxStatus;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;

@Singleton
public class VertexRemove implements ParameterizedHandler {
    private final Graph graph;
    private final WorkspaceHelper workspaceHelper;
    private final ACLProvider aclProvider;
    private final WorkQueueRepository workQueueRepository;

    @Inject
    public VertexRemove(
            final Graph graph,
            final WorkspaceHelper workspaceHelper,
            final ACLProvider aclProvider,
            final WorkQueueRepository workQueueRepository
    ) {
        this.graph = graph;
        this.workspaceHelper = workspaceHelper;
        this.aclProvider = aclProvider;
        this.workQueueRepository = workQueueRepository;
    }

    @Handle
    public ClientApiSuccess handle(
            @Required(name = "graphVertexId") String graphVertexId,
            @ActiveWorkspaceId String workspaceId,
            User user,
            Authorizations authorizations
    ) throws Exception {
        Vertex vertex = graph.getVertex(graphVertexId, authorizations);
        if (vertex == null) {
            throw new OpenLumifyResourceNotFoundException("Could not find vertex with id: " + graphVertexId);
        }

        if (!aclProvider.canDeleteElement(vertex, user, workspaceId)) {
            throw new OpenLumifyAccessDeniedException("Vertex " + graphVertexId + " is not deleteable", user,
                    graphVertexId);
        }

        SandboxStatus sandboxStatus = SandboxStatusUtil.getSandboxStatus(vertex, workspaceId);

        boolean isPublicVertex = sandboxStatus == SandboxStatus.PUBLIC;

        workspaceHelper.deleteVertex(vertex, workspaceId, isPublicVertex, Priority.HIGH, authorizations, user);
        return OpenLumifyResponse.SUCCESS;
    }
}
