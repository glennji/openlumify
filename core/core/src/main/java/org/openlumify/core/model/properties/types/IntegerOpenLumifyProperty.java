package org.openlumify.core.model.properties.types;

import org.vertexium.Element;

public class IntegerOpenLumifyProperty extends IdentityOpenLumifyProperty<Integer> {
    public IntegerOpenLumifyProperty(String propertyName) {
        super(propertyName);
    }

    public Integer getPropertyValue(Element element, String propertyKey, Integer defaultValue) {
        Integer nullable = getPropertyValue(element, propertyKey);
        if (nullable == null) {
            return defaultValue;
        }
        return nullable;
    }

    public Integer getOnlyPropertyValue(Element element, Integer defaultValue) {
        Integer nullable = getOnlyPropertyValue(element);
        if (nullable == null) {
            return defaultValue;
        }
        return nullable;
    }
}
