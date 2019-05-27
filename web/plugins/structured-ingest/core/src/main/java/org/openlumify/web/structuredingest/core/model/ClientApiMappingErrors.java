package org.openlumify.web.structuredingest.core.model;

import org.openlumify.web.clientapi.model.ClientApiObject;
import org.openlumify.web.structuredingest.core.util.mapping.EdgeMapping;
import org.openlumify.web.structuredingest.core.util.mapping.PropertyMapping;
import org.openlumify.web.structuredingest.core.util.mapping.VertexMapping;

import java.util.ArrayList;
import java.util.List;

public class ClientApiMappingErrors implements ClientApiObject {
    public List<MappingError> mappingErrors = new ArrayList<>();

    public static class MappingError {
        public PropertyMapping propertyMapping;
        public VertexMapping vertexMapping;
        public EdgeMapping edgeMapping;
        public String attribute;
        public String message;
    }
}
