package org.openlumify.core.model.properties.types;

import org.json.JSONObject;
import org.openlumify.core.util.JSONUtil;

public class JsonOpenLumifyProperty extends OpenLumifyProperty<JSONObject, String> {
    public JsonOpenLumifyProperty(String key) {
        super(key);
    }

    @Override
    public String wrap(JSONObject value) {
        return value.toString();
    }

    @Override
    public JSONObject unwrap(Object value) {
        if (value == null) {
            return null;
        }
        return JSONUtil.parse(value.toString());
    }

    @Override
    protected boolean isEquals(JSONObject newValue, JSONObject currentValue) {
        return JSONUtil.areEqual(newValue, currentValue);
    }
}
