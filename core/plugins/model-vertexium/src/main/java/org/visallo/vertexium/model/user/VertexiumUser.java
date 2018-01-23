package org.visallo.vertexium.model.user;

import com.google.common.collect.ImmutableMap;
import org.json.JSONObject;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.visallo.core.model.properties.types.SingleValueVisalloProperty;
import org.visallo.core.model.user.UserVisalloProperties;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.UserType;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class VertexiumUser implements User, Serializable {
    private static final long serialVersionUID = 6688073934273514248L;
    private final String userId;
    private final Map<String, Map<String, Object>> properties = new HashMap<>();

    public VertexiumUser(Vertex userVertex) {
        this.userId = userVertex.getId();
        for (Property property : userVertex.getProperties()) {
            Map<String, Object> propertyValues = this.properties.computeIfAbsent(property.getName(), (missingProp) -> new HashMap<>());
            propertyValues.put(property.getKey(), property.getValue());
        }
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public String getUsername() {
        return getProperty(UserVisalloProperties.USERNAME);
    }

    @Override
    public String getDisplayName() {
        return getProperty(UserVisalloProperties.DISPLAY_NAME);
    }

    @Override
    public String getEmailAddress() {
        return getProperty(UserVisalloProperties.EMAIL_ADDRESS);
    }

    @Override
    public Date getCreateDate() {
        return getProperty(UserVisalloProperties.CREATE_DATE);
    }

    @Override
    public Date getCurrentLoginDate() {
        return getProperty(UserVisalloProperties.CURRENT_LOGIN_DATE);
    }

    @Override
    public String getCurrentLoginRemoteAddr() {
        return getProperty(UserVisalloProperties.CURRENT_LOGIN_REMOTE_ADDR);
    }

    @Override
    public Date getPreviousLoginDate() {
        return getProperty(UserVisalloProperties.PREVIOUS_LOGIN_DATE);
    }

    @Override
    public String getPreviousLoginRemoteAddr() {
        return getProperty(UserVisalloProperties.PREVIOUS_LOGIN_REMOTE_ADDR);
    }

    @Override
    public int getLoginCount() {
        Integer loginCount = getProperty(UserVisalloProperties.LOGIN_COUNT);
        return loginCount == null ? 0 : loginCount;
    }

    @Override
    public UserType getUserType() {
        return UserType.USER;
    }

    @Override
    public String getCurrentWorkspaceId() {
        return getProperty(UserVisalloProperties.CURRENT_WORKSPACE);
    }

    @Override
    public JSONObject getUiPreferences() {
        JSONObject preferences = getProperty(UserVisalloProperties.UI_PREFERENCES);
        if (preferences == null) {
            preferences = new JSONObject();
            Map<String, Object> propertyValues = this.properties.computeIfAbsent(UserVisalloProperties.UI_PREFERENCES.getPropertyName(), (missingProp) -> new HashMap<>());
            propertyValues.put(DEFAULT_KEY, UserVisalloProperties.UI_PREFERENCES.wrap(preferences));
        }
        return preferences;
    }

    @Override
    public String getPasswordResetToken() {
        return getProperty(UserVisalloProperties.PASSWORD_RESET_TOKEN);
    }

    @Override
    public Date getPasswordResetTokenExpirationDate() {
        return getProperty(UserVisalloProperties.PASSWORD_RESET_TOKEN_EXPIRATION_DATE);
    }

    private <PROP_TYPE> PROP_TYPE getProperty(SingleValueVisalloProperty<PROP_TYPE, ?> property) {
        Object rawValue = getProperty(DEFAULT_KEY, property.getPropertyName());
        return rawValue == null ? null : property.unwrap(rawValue);
    }

    @Override
    public <PROP_TYPE> PROP_TYPE getProperty(String propertyName) {
        return getProperty(DEFAULT_KEY, propertyName);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <PROP_TYPE> PROP_TYPE getProperty(String key, String propertyName) {
        if (this.properties.containsKey(propertyName)) {
            return (PROP_TYPE) this.properties.get(propertyName).get(key);
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <PROP_TYPE> Map<String, PROP_TYPE> getProperties(String propertyName) {
        return (Map<String, PROP_TYPE>) this.properties.get(propertyName);
    }

    @Override
    public Map<String, Map<String, Object>> getCustomProperties() {
        Map<String, Map<String, Object>> results = new HashMap<>();
        for (Map.Entry<String, Map<String, Object>> property : properties.entrySet()) {
            if (!UserVisalloProperties.isBuiltInProperty(property.getKey())) {
                results.put(property.getKey(), ImmutableMap.copyOf(property.getValue()));
            }
        }
        return ImmutableMap.copyOf(results);
    }

    public void setProperty(String propertyName, Object value) {
        setProperty(DEFAULT_KEY, propertyName, value);
    }

    public void setProperty(String key, String propertyName, Object value) {
        Map<String, Object> propertyValues = this.properties.computeIfAbsent(propertyName, (missingProp) -> new HashMap<>());
        propertyValues.put(key, value);
    }

    public void removeProperty(String key, String propertyName) {
        if (this.properties.containsKey(propertyName)) {
            this.properties.get(propertyName).remove(key);
        }
    }

    @Override
    public String toString() {
        return "VertexiumUser{userId='" + getUserId() + "', displayName='" + getDisplayName() + "}";
    }
}
