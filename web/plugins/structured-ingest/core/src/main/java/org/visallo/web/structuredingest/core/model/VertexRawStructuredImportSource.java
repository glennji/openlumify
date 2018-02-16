package org.visallo.web.structuredingest.core.model;

import org.vertexium.Vertex;
import org.vertexium.property.StreamingPropertyValue;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.properties.VisalloProperties;

import java.io.InputStream;

public class VertexRawStructuredImportSource implements StructuredIngestInputStreamSource {

    private final Vertex vertex;

    public VertexRawStructuredImportSource(Vertex vertex) {
        this.vertex = vertex;
    }

    @Override
    public InputStream getInputStream() {
        StreamingPropertyValue rawPropertyValue = VisalloProperties.RAW.getPropertyValue(vertex);
        if (rawPropertyValue == null) {
            throw new VisalloResourceNotFoundException("Could not find raw property on vertex:" + vertex.getId());
        }
        InputStream in = rawPropertyValue.getInputStream();
        return in;
    }
}
