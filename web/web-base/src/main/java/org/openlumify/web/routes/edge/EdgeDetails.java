package org.openlumify.web.routes.edge;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Edge;
import org.vertexium.Graph;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.model.termMention.TermMentionRepository;
import org.openlumify.web.clientapi.model.ClientApiEdgeDetails;
import org.openlumify.web.clientapi.model.ClientApiSourceInfo;

@Singleton
public class EdgeDetails implements ParameterizedHandler {
    private final Graph graph;
    private final TermMentionRepository termMentionRepository;

    @Inject
    public EdgeDetails(
            Graph graph,
            TermMentionRepository termMentionRepository
    ) {
        this.graph = graph;
        this.termMentionRepository = termMentionRepository;
    }

    @Handle
    public ClientApiEdgeDetails handle(
            @Required(name = "edgeId") String edgeId,
            Authorizations authorizations
    ) throws Exception {
        Edge edge = this.graph.getEdge(edgeId, authorizations);
        if (edge == null) {
            throw new OpenLumifyResourceNotFoundException("Could not find edge with id: " + edgeId, edgeId);
        }

        ClientApiSourceInfo sourceInfo = termMentionRepository.getSourceInfoForEdge(edge, authorizations);

        ClientApiEdgeDetails result = new ClientApiEdgeDetails();
        result.sourceInfo = sourceInfo;
        return result;
    }
}
