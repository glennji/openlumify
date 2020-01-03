package org.openlumify.web.routes.vertex;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.model.termMention.TermMentionRepository;
import org.openlumify.web.OpenLumifyResponse;
import org.openlumify.web.clientapi.model.ClientApiSourceInfo;
import org.openlumify.web.clientapi.model.ClientApiVertexDetails;

@Singleton
public class VertexDetails implements ParameterizedHandler {
    private final Graph graph;
    private final TermMentionRepository termMentionRepository;

    @Inject
    public VertexDetails(
            Graph graph,
            TermMentionRepository termMentionRepository
    ) {
        this.graph = graph;
        this.termMentionRepository = termMentionRepository;
    }

    @Handle
    public ClientApiVertexDetails handle(
            @Required(name = "vertexId") String vertexId,
            Authorizations authorizations,
            OpenLumifyResponse response
    ) throws Exception {
        Vertex vertex = this.graph.getVertex(vertexId, authorizations);
        if (vertex == null) {
            throw new OpenLumifyResourceNotFoundException("Could not find vertex with id: " + vertexId);
        }

        ClientApiSourceInfo sourceInfo = termMentionRepository.getSourceInfoForVertex(vertex, authorizations);

        ClientApiVertexDetails result = new ClientApiVertexDetails();
        result.sourceInfo = sourceInfo;
        return result;
    }
}
