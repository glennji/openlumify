package org.openlumify.web.routes.vertex;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.openlumify.core.model.properties.OpenLumifyProperties;
import org.openlumify.web.clientapi.model.ClientApiVertexCountsByConceptType;

import java.util.Map;

@Singleton
public class VertexGetCountsByConceptType implements ParameterizedHandler {
    private final Graph graph;

    @Inject
    public VertexGetCountsByConceptType(Graph graph) {
        this.graph = graph;
    }

    @Handle
    public ClientApiVertexCountsByConceptType handle(
            Authorizations authorizations
    ) throws Exception {
        Map<Object, Long> conceptTypeCounts = graph.getVertexPropertyCountByValue(OpenLumifyProperties.CONCEPT_TYPE.getPropertyName(), authorizations);
        return new ClientApiVertexCountsByConceptType(conceptTypeCounts);
    }
}
