package org.openlumify.core.model.properties.types;

import org.json.JSONObject;

public class JsonOpenLumifyExtendedData extends OpenLumifyExtendedData<JSONObject, String> {
    public JsonOpenLumifyExtendedData(String tableName, String columnName) {
        super(tableName, columnName);
    }

    @Override
    public String rawToGraph(JSONObject value) {
        return value.toString();
    }

    @Override
    public JSONObject graphToRaw(Object value) {
        return new JSONObject(value.toString());
    }
}
