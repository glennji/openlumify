package org.openlumify.web.routes.vertex;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.webster.ParameterizedHandler;
import org.openlumify.webster.annotations.Handle;
import org.openlumify.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.util.ClientApiConverter;
import org.openlumify.web.clientapi.model.ClientApiElement;
import org.openlumify.web.clientapi.model.ClientApiVertex;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;

@Singleton
public class VertexProperties implements ParameterizedHandler {
    private final Graph graph;

    @Inject
    public VertexProperties(final Graph graph) {
        this.graph = graph;
    }

    @Handle
    public ClientApiVertex handle(
            @Required(name = "graphVertexId") String graphVertexId,
            @ActiveWorkspaceId String workspaceId,
            Authorizations authorizations
    ) throws Exception {
        Vertex vertex = graph.getVertex(graphVertexId, authorizations);
        if (vertex == null) {
            throw new OpenLumifyResourceNotFoundException("Could not find vertex: " + graphVertexId);
        }
        return (ClientApiVertex) ClientApiConverter.toClientApi(vertex, workspaceId, authorizations);
    }
}
