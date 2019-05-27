package org.openlumify.vertexium.model.user;

import com.google.common.collect.ImmutableMap;
import org.json.JSONObject;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.openlumify.core.model.user.UserOpenLumifyProperties;
import org.openlumify.core.user.User;
import org.openlumify.web.clientapi.model.UserType;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class VertexiumUser implements User, Serializable {
    private static final long serialVersionUID = 6688073934273514248L;
    private final String userId;
    private final Map<String, Object> properties = new HashMap<>();

    public VertexiumUser(Vertex userVertex) {
        this.userId = userVertex.getId();
        for (Property property : userVertex.getProperties()) {
            this.properties.put(property.getName(), property.getValue());
        }
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public String getUsername() {
        return UserOpenLumifyProperties.USERNAME.getPropertyValue(properties);
    }

    @Override
    public String getDisplayName() {
        return UserOpenLumifyProperties.DISPLAY_NAME.getPropertyValue(properties);
    }

    @Override
    public String getEmailAddress() {
        return UserOpenLumifyProperties.EMAIL_ADDRESS.getPropertyValue(properties);
    }

    @Override
    public Date getCreateDate() {
        return UserOpenLumifyProperties.CREATE_DATE.getPropertyValue(properties);
    }

    @Override
    public Date getCurrentLoginDate() {
        return UserOpenLumifyProperties.CURRENT_LOGIN_DATE.getPropertyValue(properties);
    }

    @Override
    public String getCurrentLoginRemoteAddr() {
        return UserOpenLumifyProperties.CURRENT_LOGIN_REMOTE_ADDR.getPropertyValue(properties);
    }

    @Override
    public Date getPreviousLoginDate() {
        return UserOpenLumifyProperties.PREVIOUS_LOGIN_DATE.getPropertyValue(properties);
    }

    @Override
    public String getPreviousLoginRemoteAddr() {
        return UserOpenLumifyProperties.PREVIOUS_LOGIN_REMOTE_ADDR.getPropertyValue(properties);
    }

    @Override
    public int getLoginCount() {
        return UserOpenLumifyProperties.LOGIN_COUNT.getPropertyValue(properties, 0);
    }

    @Override
    public UserType getUserType() {
        return UserType.USER;
    }

    @Override
    public String getCurrentWorkspaceId() {
        return UserOpenLumifyProperties.CURRENT_WORKSPACE.getPropertyValue(properties);
    }

    @Override
    public JSONObject getUiPreferences() {
        JSONObject preferences = UserOpenLumifyProperties.UI_PREFERENCES.getPropertyValue(properties);
        if (preferences == null) {
            preferences = new JSONObject();
            UserOpenLumifyProperties.UI_PREFERENCES.setProperty(properties, preferences);
        }
        return preferences;
    }

    @Override
    public String getPasswordResetToken() {
        return UserOpenLumifyProperties.PASSWORD_RESET_TOKEN.getPropertyValue(properties);
    }

    @Override
    public Date getPasswordResetTokenExpirationDate() {
        return UserOpenLumifyProperties.PASSWORD_RESET_TOKEN_EXPIRATION_DATE.getPropertyValue(properties);
    }

    @Override
    public Object getProperty(String propertyName) {
        return this.properties.get(propertyName);
    }

    @Override
    public Map<String, Object> getCustomProperties() {
        Map<String, Object> results = new HashMap<>();
        for (Map.Entry<String, Object> property : properties.entrySet()) {
            if (!UserOpenLumifyProperties.isBuiltInProperty(property.getKey())) {
                results.put(property.getKey(), property.getValue());
            }
        }
        return ImmutableMap.copyOf(results);
    }

    public void setProperty(String propertyName, Object value) {
        this.properties.put(propertyName, value);
    }

    @Override
    public String toString() {
        return "VertexiumUser{userId='" + getUserId() + "', displayName='" + getDisplayName() + "}";
    }
}
