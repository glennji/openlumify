package org.openlumify.web.structuredingest.core.routes;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.webster.ParameterizedHandler;
import org.openlumify.webster.annotations.Handle;
import org.openlumify.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.vertexium.property.StreamingPropertyValue;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.model.properties.OpenLumifyProperties;
import org.openlumify.web.structuredingest.core.model.ClientApiAnalysis;
import org.openlumify.web.structuredingest.core.model.StructuredIngestParser;
import org.openlumify.web.structuredingest.core.util.StructuredIngestParserFactory;

import java.io.InputStream;
import java.util.List;

@Singleton
public class Analyze implements ParameterizedHandler {
    private final Graph graph;
    private final StructuredIngestParserFactory structuredIngestParserFactory;

    @Inject
    public Analyze(Graph graph, StructuredIngestParserFactory structuredIngestParserFactory) {
        this.graph = graph;
        this.structuredIngestParserFactory = structuredIngestParserFactory;
    }

    @Handle
    public ClientApiAnalysis handle(
            Authorizations authorizations,
            @Required(name = "graphVertexId") String graphVertexId
    ) throws Exception {
        Vertex vertex = graph.getVertex(graphVertexId, authorizations);
        if (vertex == null) {
            throw new OpenLumifyResourceNotFoundException("Could not find vertex:" + graphVertexId);
        }

        StreamingPropertyValue rawPropertyValue = OpenLumifyProperties.RAW.getPropertyValue(vertex);
        if (rawPropertyValue == null) {
            throw new OpenLumifyResourceNotFoundException("Could not find raw property on vertex:" + graphVertexId);
        }

        List <String> mimeTypes = Lists.newArrayList(OpenLumifyProperties.MIME_TYPE.getPropertyValues(vertex));
        for (String mimeType : mimeTypes) {
            StructuredIngestParser parser = structuredIngestParserFactory.getParser(mimeType);
            if (parser != null) {
                try (InputStream inputStream = rawPropertyValue.getInputStream()) {
                    return parser.analyze(inputStream);
                }
            }
        }

        return null;
    }
}
