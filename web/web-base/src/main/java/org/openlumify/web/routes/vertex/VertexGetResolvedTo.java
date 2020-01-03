package org.openlumify.web.routes.vertex;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Optional;
import org.visallo.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.model.termMention.TermMentionRepository;
import org.openlumify.web.clientapi.model.ClientApiTermMentionsResponse;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class VertexGetResolvedTo implements ParameterizedHandler {
    private final Graph graph;
    private final TermMentionRepository termMentionRepository;

    @Inject
    public VertexGetResolvedTo(
            Graph graph,
            TermMentionRepository termMentionRepository
    ) {
        this.graph = graph;
        this.termMentionRepository = termMentionRepository;
    }

    @Handle
    public ClientApiTermMentionsResponse handle(
            @Required(name = "graphVertexId") String graphVertexId,
            @Optional(name = "propertyKey") String propertyKey,
            @Optional(name = "propertyName") String propertyName,
            @ActiveWorkspaceId String workspaceId,
            Authorizations authorizations
    ) throws Exception {
        Vertex vertex = graph.getVertex(graphVertexId, authorizations);
        if (vertex == null) {
            throw new OpenLumifyResourceNotFoundException(String.format("vertex %s not found", graphVertexId));
        }

        Stream<Vertex> termMentions;
        if (propertyKey != null || propertyName != null) {
            Property property = vertex.getProperty(propertyKey, propertyName);
            if (property == null) {
                throw new OpenLumifyResourceNotFoundException(String.format(
                        "property %s:%s not found on vertex %s",
                        propertyKey,
                        propertyName,
                        vertex.getId()
                ));
            }
            termMentions = termMentionRepository.findResolvedToForRef(
                    graphVertexId,
                    propertyKey,
                    propertyName,
                    authorizations
            );
        } else {
            termMentions = termMentionRepository.findResolvedToForRefElement(graphVertexId, authorizations);
        }

        return termMentionRepository.toClientApi(
                termMentions.collect(Collectors.toList()),
                workspaceId,
                authorizations
        );
    }
}
