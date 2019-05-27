package org.openlumify.core.model.properties.types;

import org.openlumify.core.ingest.ArtifactDetectedObject;
import org.json.JSONObject;

public class DetectedObjectProperty extends OpenLumifyProperty<ArtifactDetectedObject, String> {
    public DetectedObjectProperty(String key) {
        super(key);
    }

    @Override
    public String wrap(ArtifactDetectedObject value) {
        return value.toJson().toString();
    }

    @Override
    public ArtifactDetectedObject unwrap(Object value) {
        if (value == null) {
            return null;
        }
        return new ArtifactDetectedObject(new JSONObject(value.toString()));
    }
}
