package org.openlumify.core.model.properties.types;

import org.vertexium.Element;

public class LongSingleValueOpenLumifyProperty extends IdentitySingleValueOpenLumifyProperty<Long> {
    public LongSingleValueOpenLumifyProperty(String key) {
        super(key);
    }

    public long getPropertyValue(Element element, long defaultValue) {
        Long nullable = getPropertyValue(element);
        if (nullable == null) {
            return defaultValue;
        }
        return nullable;
    }
}
