package org.openlumify.core.model.properties.types;

import org.vertexium.Element;

public class DoubleOpenLumifyProperty extends IdentityOpenLumifyProperty<Double> {
    public DoubleOpenLumifyProperty(String key) {
        super(key);
    }

    public Double getPropertyValue(Element element, String propertyKey, Double defaultValue) {
        Double nullable = getPropertyValue(element, propertyKey);
        if (nullable == null) {
            return defaultValue;
        }
        return nullable;
    }

    public Double getFirstPropertyValue(Element element, Double defaultValue) {
        Double nullable = getOnlyPropertyValue(element);
        if (nullable == null) {
            return defaultValue;
        }
        return nullable;
    }
}
