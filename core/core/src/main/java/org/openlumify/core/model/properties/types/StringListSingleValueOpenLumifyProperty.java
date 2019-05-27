package org.openlumify.core.model.properties.types;

import org.json.JSONArray;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.util.JSONUtil;

import java.util.List;

public class StringListSingleValueOpenLumifyProperty extends SingleValueOpenLumifyProperty<List<String>, String> {
    public StringListSingleValueOpenLumifyProperty(String key) {
        super(key);
    }

    @Override
    public String wrap(List<String> value) {
        return new JSONArray(value).toString();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> unwrap(Object value) {
        if (value == null) {
            return null;
        }

        List<String> valueList;
        if (value instanceof String) {
            valueList = JSONUtil.toStringList(JSONUtil.parseArray(value.toString()));
        } else if (value instanceof List) {
            valueList = (List<String>) value;
        } else {
            throw new OpenLumifyException("Could not unwrap type: " + value.getClass().getName());
        }

        return valueList;
    }
}
