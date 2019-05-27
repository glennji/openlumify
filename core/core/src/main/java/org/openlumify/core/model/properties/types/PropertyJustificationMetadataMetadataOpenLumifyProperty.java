package org.openlumify.core.model.properties.types;

import org.openlumify.core.model.PropertyJustificationMetadata;
import org.json.JSONObject;

public class PropertyJustificationMetadataMetadataOpenLumifyProperty extends MetadataOpenLumifyProperty<PropertyJustificationMetadata, String> {
    public PropertyJustificationMetadataMetadataOpenLumifyProperty(final String key) {
        super(key);
    }

    @Override
    public String wrap(PropertyJustificationMetadata value) {
        return value.toJson().toString();
    }

    @Override
    public PropertyJustificationMetadata unwrap(Object value) {
        if (value == null) {
            return null;
        }
        return new PropertyJustificationMetadata(new JSONObject(value.toString()));
    }
}
