package org.openlumify.web.parameterValueConverters;

import org.openlumify.webster.DefaultParameterValueConverter;
import org.json.JSONObject;
import org.openlumify.core.exception.OpenLumifyException;

public class JSONObjectParameterValueConverter implements DefaultParameterValueConverter.Converter<JSONObject> {
    @Override
    public JSONObject convert(Class parameterType, String parameterName, String[] value) {
        try {
            return new JSONObject(value[0]);
        } catch (Exception ex) {
            throw new OpenLumifyException("Could not parse JSONObject: " + value[0]);
        }
    }
}
