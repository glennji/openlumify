package org.openlumify.web.routes.vertex;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.webster.ParameterizedHandler;
import org.openlumify.webster.annotations.Handle;
import org.openlumify.webster.annotations.Optional;
import org.openlumify.webster.annotations.Required;
import org.vertexium.*;
import org.vertexium.query.QueryResultsIterable;
import org.vertexium.query.VertexQuery;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.trace.Trace;
import org.openlumify.core.trace.TraceSpan;
import org.openlumify.core.util.ClientApiConverter;
import org.openlumify.web.clientapi.model.ClientApiVertex;
import org.openlumify.web.clientapi.model.ClientApiVertexEdges;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;

@Singleton
public class VertexEdges implements ParameterizedHandler {
    private final Graph graph;

    @Inject
    public VertexEdges(final Graph graph) {
        this.graph = graph;
    }

    @Handle
    public ClientApiVertexEdges handle(
            @Required(name = "graphVertexId") String graphVertexId,
            @Optional(name = "offset", defaultValue = "0") int offset,
            @Optional(name = "size", defaultValue = "25") int size,
            @Optional(name = "edgeLabel") String edgeLabel,
            @Optional(name = "relatedVertexId") String relatedVertexId,
            @Optional(name = "direction", defaultValue = "BOTH") String directionStr,
            @ActiveWorkspaceId String workspaceId,
            Authorizations authorizations
    ) throws Exception {
        Vertex vertex;
        Vertex relatedVertex = null;
        try (TraceSpan trace = Trace.start("getOriginalVertex").data("graphVertexId", graphVertexId)) {
            vertex = graph.getVertex(graphVertexId, authorizations);
            if (vertex == null) {
                throw new OpenLumifyResourceNotFoundException("Could not find vertex: " + graphVertexId);
            }
        }

        if (relatedVertexId != null) {
            relatedVertex = graph.getVertex(relatedVertexId, authorizations);
            if (relatedVertex == null) {
                throw new OpenLumifyResourceNotFoundException("Could not find related vertex: " + relatedVertexId);
            }
        }

        VertexQuery edgesQuery = vertex.query(authorizations);

        if (!Strings.isNullOrEmpty(edgeLabel)) {
            edgesQuery.hasEdgeLabel(edgeLabel);
        }

        Direction direction = Direction.valueOf(directionStr.toUpperCase());
        edgesQuery.hasDirection(direction);

        if (!Strings.isNullOrEmpty(relatedVertexId)) {
            edgesQuery.hasOtherVertexId(relatedVertexId);
        }

        QueryResultsIterable<Edge> edges = edgesQuery
                .skip(offset)
                .limit(size)
                .edges();

        ClientApiVertexEdges result = new ClientApiVertexEdges();

        for (Edge edge : edges) {
            String otherVertexId = edge.getOtherVertexId(graphVertexId);
            Vertex otherVertex = graph.getVertex(otherVertexId, authorizations);
            if (otherVertex == null) {
                continue;
            }

            result.getRelationships().add(convertEdgeToClientApi(edge, otherVertex, workspaceId, authorizations));
        }

        result.setTotalReferences(edges.getTotalHits());

        return result;
    }

    /**
     * This is overridable so web plugins can modify the resulting set of edges.
     */
    protected ClientApiVertexEdges.Edge convertEdgeToClientApi(Edge edge, Vertex otherVertex, String workspaceId, Authorizations authorizations) {
        ClientApiVertexEdges.Edge clientApiEdge = new ClientApiVertexEdges.Edge();
        clientApiEdge.setRelationship(ClientApiConverter.toClientApiEdge(edge, workspaceId));

        ClientApiVertex clientApiVertex;
        clientApiVertex = ClientApiConverter.toClientApiVertex(otherVertex, workspaceId, authorizations);
        clientApiEdge.setVertex(clientApiVertex);

        return clientApiEdge;
    }
}
