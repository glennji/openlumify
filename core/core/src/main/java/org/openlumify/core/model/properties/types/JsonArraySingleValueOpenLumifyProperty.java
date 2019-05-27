package org.openlumify.core.model.properties.types;

import org.json.JSONArray;
import org.openlumify.core.util.JSONUtil;

public class JsonArraySingleValueOpenLumifyProperty extends SingleValueOpenLumifyProperty<JSONArray, String> {
    public JsonArraySingleValueOpenLumifyProperty(String key) {
        super(key);
    }

    @Override
    public String wrap(JSONArray value) {
        return value.toString();
    }

    @Override
    public JSONArray unwrap(Object value) {
        if (value == null) {
            return null;
        }
        return JSONUtil.parseArray(value.toString());
    }

    @Override
    protected boolean isEquals(JSONArray newValue, JSONArray currentValue) {
        return JSONUtil.areEqual(newValue, currentValue);
    }
}
