package org.openlumify.core.model.properties.types;

import org.vertexium.Element;

public class LongOpenLumifyProperty extends IdentityOpenLumifyProperty<Long> {
    public LongOpenLumifyProperty(String key) {
        super(key);
    }

    public long getPropertyValue(Element element, String propertyKey, long defaultValue) {
        Long nullable = getPropertyValue(element, propertyKey);
        if (nullable == null) {
            return defaultValue;
        }
        return nullable;
    }
}
