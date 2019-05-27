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
import org.vertexium.util.IterableUtils;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.util.ClientApiConverter;
import org.openlumify.web.clientapi.model.ClientApiDetectedObjects;

// This route will no longer be needed once we refactor detected objects.
@Singleton
public class VertexGetDetectedObjects implements ParameterizedHandler {
    private final Graph graph;

    @Inject
    public VertexGetDetectedObjects(
            Graph graph
    ) {
        this.graph = graph;
    }

    @Handle
    public ClientApiDetectedObjects handle(
            @Required(name = "graphVertexId") String graphVertexId,
            @Required(name = "propertyName") String propertyName,
            @Required(name = "workspaceId") String workspaceId,
            Authorizations authorizations
    ) throws Exception {
        Vertex vertex = graph.getVertex(graphVertexId, authorizations);
        if (vertex == null) {
            throw new OpenLumifyResourceNotFoundException(String.format("vertex %s not found", graphVertexId));
        }

        ClientApiDetectedObjects detectedObjects = new ClientApiDetectedObjects();
        Iterable<Property> detectedObjectProperties = vertex.getProperties(propertyName);
        if (detectedObjectProperties == null || IterableUtils.count(detectedObjectProperties) == 0) {
            throw new OpenLumifyResourceNotFoundException(String.format("property %s not found on vertex %s", propertyName, vertex.getId()));
        }
        detectedObjects.addDetectedObjects(ClientApiConverter.toClientApiProperties(detectedObjectProperties, workspaceId));

        return detectedObjects;
    }

}
