package org.openlumify.core.model.properties.types;

import java.util.Map;

public class IntegerSingleValueOpenLumifyProperty extends IdentitySingleValueOpenLumifyProperty<Integer> {
    public IntegerSingleValueOpenLumifyProperty(String key) {
        super(key);
    }

    public Integer getPropertyValue(Map<String, Object> map, Integer defaultValue) {
        Integer nullable = getPropertyValue(map);
        if (nullable == null) {
            return defaultValue;
        }
        return nullable;
    }
}
