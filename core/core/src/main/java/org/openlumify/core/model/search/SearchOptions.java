package org.openlumify.core.model.search;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openlumify.core.exception.OpenLumifyException;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class SearchOptions {
    private final Map<String, Object> parameters;
    private final String workspaceId;

    public SearchOptions(Map<String, Object> parameters, String workspaceId) {
        this.parameters = parameters;
        this.workspaceId = workspaceId;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public <T> T getOptionalParameter(String parameterName, Class<T> resultType) {
        Object obj = parameters.get(parameterName);
        if (obj == null) {
            return null;
        }
        try {
            if (resultType.isArray() && obj instanceof Collection) {
                Collection collection = (Collection) obj;
                Class type = resultType.getComponentType();
                return (T) collection.toArray((Object[]) Array.newInstance(type, collection.size()));
            } else if (resultType.isArray() && !obj.getClass().isArray()) {
                Object[] array = (Object[]) Array.newInstance(resultType.getComponentType(), 1);
                array[0] = objectToType(obj, resultType.getComponentType());
                return objectToType(array, resultType);
            }
            return objectToType(obj, resultType);
        } catch (Exception ex) {
            throw new OpenLumifyException("Could not cast object \"" + obj + "\" to type \"" + resultType.getName() + "\"", ex);
        }
    }

    private <T> T objectToType(Object obj, Class<T> resultType) {
        if (obj != null && resultType == obj.getClass()) {
            //noinspection unchecked
            return (T) obj;
        }
        if (resultType == Integer.class && obj instanceof String) {
            return resultType.cast(Integer.parseInt((String) obj));
        }
        if (resultType == Long.class && obj instanceof String) {
            return resultType.cast(Long.parseLong((String) obj));
        }
        if (resultType == Long.class && obj instanceof Integer) {
            return resultType.cast(((Integer) obj).longValue());
        }
        if (resultType == Double.class && obj instanceof String) {
            return resultType.cast(Double.parseDouble((String) obj));
        }
        if (resultType == Float.class && obj instanceof String) {
            return resultType.cast(Float.parseFloat((String) obj));
        }
        if (resultType == JSONArray.class && obj instanceof String) {
            return resultType.cast(new JSONArray((String) obj));
        }
        if (resultType == JSONArray.class && obj instanceof String[]) {
            return resultType.cast(new JSONArray(obj));
        }
        if (resultType == Boolean.class && obj instanceof String) {
            return resultType.cast(Boolean.parseBoolean((String) obj));
        }
        if (resultType == String.class && obj instanceof JSONObject) {
            return resultType.cast(obj.toString());
        }
        return resultType.cast(obj);
    }

    public <T> T getOptionalParameter(String parameterName, T defaultValue) {
        checkNotNull(defaultValue, "defaultValue cannot be null");
        T obj = (T) getOptionalParameter(parameterName, defaultValue.getClass());
        if (obj == null) {
            // null is a possible value, for example limit=null signifies don't limit the results. If limit is
            // not specified use the defaultValue
            if (parameters.containsKey(parameterName)) {
                return null;
            }
            return defaultValue;
        }
        return obj;
    }

    public <T> T getRequiredParameter(String parameterName, Class<T> resultType) {
        T obj = getOptionalParameter(parameterName, resultType);
        if (obj == null) {
            throw new OpenLumifyException("Missing parameter: " + parameterName);
        }
        return obj;
    }
}
