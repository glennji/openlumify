package org.openlumify.web.routes.vertex;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.webster.ParameterizedHandler;
import org.openlumify.webster.annotations.Handle;
import org.openlumify.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.model.termMention.TermMentionRepository;
import org.openlumify.web.clientapi.model.ClientApiTermMentionsResponse;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;

@Singleton
public class VertexGetTermMentions implements ParameterizedHandler {
    private final Graph graph;
    private final TermMentionRepository termMentionRepository;

    @Inject
    public VertexGetTermMentions(
            Graph graph,
            TermMentionRepository termMentionRepository
    ) {
        this.graph = graph;
        this.termMentionRepository = termMentionRepository;
    }

    @Handle
    public ClientApiTermMentionsResponse handle(
            @Required(name = "graphVertexId") String graphVertexId,
            @Required(name = "propertyKey") String propertyKey,
            @Required(name = "propertyName") String propertyName,
            @ActiveWorkspaceId String workspaceId,
            Authorizations authorizations
    ) throws Exception {
        Vertex vertex = graph.getVertex(graphVertexId, authorizations);
        if (vertex == null) {
            throw new OpenLumifyResourceNotFoundException(String.format("vertex %s not found", graphVertexId));
        }

        Property property = vertex.getProperty(propertyKey, propertyName);
        if (property == null) {
            throw new OpenLumifyResourceNotFoundException(String.format(
                    "property %s:%s not found on vertex %s",
                    propertyKey,
                    propertyName,
                    vertex.getId()
            ));
        }

        Iterable<Vertex> termMentions = termMentionRepository.findByOutVertexAndProperty(
                graphVertexId,
                propertyKey,
                propertyName,
                authorizations
        );
        return termMentionRepository.toClientApi(termMentions, workspaceId, authorizations);
    }
}
