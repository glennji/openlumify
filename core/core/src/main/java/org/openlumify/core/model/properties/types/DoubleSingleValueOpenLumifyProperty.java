package org.openlumify.core.model.properties.types;

import org.vertexium.Element;

public class DoubleSingleValueOpenLumifyProperty extends IdentitySingleValueOpenLumifyProperty<Double> {
    public DoubleSingleValueOpenLumifyProperty(String key) {
        super(key);
    }

    public double getPropertyValue(Element element, double defaultValue) {
        Double nullable = getPropertyValue(element);
        if (nullable == null) {
            return defaultValue;
        }
        return nullable;
    }
}
