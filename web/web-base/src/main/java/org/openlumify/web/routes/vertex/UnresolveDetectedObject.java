package org.openlumify.web.routes.vertex;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.webster.ParameterizedHandler;
import org.openlumify.webster.annotations.Handle;
import org.openlumify.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Edge;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.openlumify.core.ingest.ArtifactDetectedObject;
import org.openlumify.core.model.properties.OpenLumifyProperties;
import org.openlumify.core.model.workQueue.Priority;
import org.openlumify.core.model.workQueue.WorkQueueRepository;
import org.openlumify.core.model.workspace.WorkspaceHelper;
import org.openlumify.core.user.User;
import org.openlumify.core.util.ClientApiConverter;
import org.openlumify.core.util.SandboxStatusUtil;
import org.openlumify.web.BadRequestException;
import org.openlumify.web.clientapi.model.ClientApiElement;
import org.openlumify.web.clientapi.model.ClientApiVertex;
import org.openlumify.web.clientapi.model.SandboxStatus;
import org.openlumify.web.clientapi.model.VisibilityJson;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;

@Singleton
public class UnresolveDetectedObject implements ParameterizedHandler {
    private final Graph graph;
    private final WorkQueueRepository workQueueRepository;
    private final WorkspaceHelper workspaceHelper;

    @Inject
    public UnresolveDetectedObject(
            final Graph graph,
            WorkQueueRepository workQueueRepository,
            final WorkspaceHelper workspaceHelper
    ) {
        this.graph = graph;
        this.workQueueRepository = workQueueRepository;
        this.workspaceHelper = workspaceHelper;
    }

    @Handle
    public ClientApiVertex handle(
            @Required(name = "vertexId") String vertexId,
            @Required(name = "multiValueKey") String multiValueKey,
            @ActiveWorkspaceId String workspaceId,
            User user,
            Authorizations authorizations
    ) throws Exception {
        Vertex artifactVertex = graph.getVertex(vertexId, authorizations);
        ArtifactDetectedObject artifactDetectedObject = OpenLumifyProperties.DETECTED_OBJECT.getPropertyValue(artifactVertex, multiValueKey);
        Edge edge = graph.getEdge(artifactDetectedObject.getEdgeId(), authorizations);
        Vertex resolvedVertex = edge.getOtherVertex(artifactVertex.getId(), authorizations);

        SandboxStatus edgeSandboxStatus = SandboxStatusUtil.getSandboxStatus(edge, workspaceId);
        boolean isPublicEdge = edgeSandboxStatus == SandboxStatus.PUBLIC;

        workspaceHelper.deleteEdge(workspaceId, edge, artifactVertex, resolvedVertex, isPublicEdge, Priority.HIGH, authorizations, user);

        return (ClientApiVertex) ClientApiConverter.toClientApi(artifactVertex, workspaceId, authorizations);
    }
}
