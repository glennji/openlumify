package org.openlumify.core.model.properties.types;

import org.json.JSONObject;
import org.openlumify.core.util.JSONUtil;

public class JsonOpenLumifyExtendedDataProperty extends OpenLumifyExtendedData<JSONObject, String> {
    public JsonOpenLumifyExtendedDataProperty(String tableName, String columnName) {
        super(tableName, columnName);
    }

    @Override
    public String rawToGraph(JSONObject value) {
        return value.toString();
    }

    @Override
    public JSONObject graphToRaw(Object value) {
        if (value == null) {
            return null;
        }
        return JSONUtil.parse(value.toString());
    }
}
