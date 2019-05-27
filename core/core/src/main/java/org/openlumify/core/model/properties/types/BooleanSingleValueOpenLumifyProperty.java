package org.openlumify.core.model.properties.types;

import org.vertexium.Element;

public class BooleanSingleValueOpenLumifyProperty extends IdentitySingleValueOpenLumifyProperty<Boolean> {
    public BooleanSingleValueOpenLumifyProperty(String key) {
        super(key);
    }

    public boolean getPropertyValue(Element element, boolean defaultValue) {
        Boolean nullable = getPropertyValue(element);
        if (nullable == null) {
            return defaultValue;
        }
        return nullable;
    }
}
