package org.openlumify.web.routes.vertex;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.webster.ParameterizedHandler;
import org.openlumify.webster.annotations.Handle;
import org.openlumify.webster.annotations.Optional;
import org.openlumify.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.model.longRunningProcess.FindPathLongRunningProcessQueueItem;
import org.openlumify.core.model.longRunningProcess.LongRunningProcessRepository;
import org.openlumify.core.user.User;
import org.openlumify.web.OpenLumifyResponse;
import org.openlumify.web.clientapi.model.ClientApiLongRunningProcessSubmitResponse;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;

@Singleton
public class VertexFindPath implements ParameterizedHandler {
    private final Graph graph;
    private final LongRunningProcessRepository longRunningProcessRepository;

    @Inject
    public VertexFindPath(
            final Graph graph,
            final LongRunningProcessRepository longRunningProcessRepository
    ) {
        this.graph = graph;
        this.longRunningProcessRepository = longRunningProcessRepository;
    }

    @Handle
    public ClientApiLongRunningProcessSubmitResponse handle(
            User user,
            @ActiveWorkspaceId String workspaceId,
            @Required(name = "outVertexId") String outVertexId,
            @Required(name = "inVertexId") String inVertexId,
            @Required(name = "hops") int hops,
            @Optional(name = "edgeLabels[]") String[] edgeLabels,
            Authorizations authorizations,
            OpenLumifyResponse response
    ) throws Exception {
        Vertex outVertex = graph.getVertex(outVertexId, authorizations);
        if (outVertex == null) {
            throw new OpenLumifyResourceNotFoundException("Source vertex not found");
        }

        Vertex inVertex = graph.getVertex(inVertexId, authorizations);
        if (inVertex == null) {
            throw new OpenLumifyResourceNotFoundException("Destination vertex not found");
        }

        FindPathLongRunningProcessQueueItem findPathQueueItem = new FindPathLongRunningProcessQueueItem(outVertex.getId(), inVertex.getId(), edgeLabels, hops, workspaceId, authorizations);
        String id = this.longRunningProcessRepository.enqueue(findPathQueueItem.toJson(), user, authorizations);

        return new ClientApiLongRunningProcessSubmitResponse(id);
    }
}

