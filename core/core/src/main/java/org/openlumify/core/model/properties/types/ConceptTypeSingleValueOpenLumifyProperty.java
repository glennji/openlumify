package org.openlumify.core.model.properties.types;

import org.vertexium.Element;
import org.openlumify.core.model.properties.OpenLumifyProperties;
import org.openlumify.web.clientapi.model.ClientApiElement;

public class ConceptTypeSingleValueOpenLumifyProperty extends StringSingleValueOpenLumifyProperty {
    public ConceptTypeSingleValueOpenLumifyProperty(String key) {
        super(key);
    }

    @Override
    public String getPropertyValue(Element element) {
        String propertyValue = super.getPropertyValue(element);
        return propertyValue == null ? OpenLumifyProperties.CONCEPT_TYPE_THING : propertyValue;
    }

    @Override
    public String getPropertyValue(ClientApiElement element) {
        String propertyValue = super.getPropertyValue(element);
        return propertyValue == null ? OpenLumifyProperties.CONCEPT_TYPE_THING : propertyValue;
    }

    public boolean hasConceptType(Element element) {
        String propertyValue = super.getPropertyValue(element);
        return propertyValue != null;
    }
}
