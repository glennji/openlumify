package org.openlumify.core.model.properties.types;

import org.vertexium.Property;

public class StringSingleValueOpenLumifyProperty extends SingleValueOpenLumifyProperty<String, String> {
    public StringSingleValueOpenLumifyProperty(final String key) {
        super(key);
    }

    @Override
    public String wrap(final String value) {
        return value;
    }

    @Override
    public String unwrap(final Object value) {
        return value.toString();
    }

    public static String getValue(Property property) {
        Object value = property.getValue();
        if (value == null) {
            return null;
        }
        return value.toString();
    }
}

