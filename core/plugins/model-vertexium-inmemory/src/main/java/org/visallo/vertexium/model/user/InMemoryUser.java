package org.visallo.vertexium.model.user;

import com.google.common.collect.ImmutableMap;
import org.json.JSONObject;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.UserType;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InMemoryUser implements User {
    private final String userId;
    private final String userName;
    private final String displayName;
    private final String emailAddress;
    private final Date createDate;
    private final String currentWorkspaceId;
    private JSONObject preferences;
    private Map<String, Map<String, Object>> properties = new HashMap<>();

    public InMemoryUser(String userId) {
        this(userId, null, null, null, null);
    }

    public InMemoryUser(
            String userName,
            String displayName,
            String emailAddress,
            String currentWorkspaceId
    ) {
        this(UUID.randomUUID().toString(), userName, displayName, emailAddress, currentWorkspaceId);
    }

    public InMemoryUser(
            String userId,
            String userName,
            String displayName,
            String emailAddress,
            String currentWorkspaceId
    ) {
        this.userId = userId;
        this.userName = userName;
        this.displayName = displayName;
        this.emailAddress = emailAddress;
        this.createDate = new Date();
        this.currentWorkspaceId = currentWorkspaceId;
        this.preferences = new JSONObject();
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public String getUsername() {
        return userName;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getEmailAddress() {
        return emailAddress;
    }

    @Override
    public Date getCreateDate() {
        return createDate;
    }

    @Override
    public Date getCurrentLoginDate() {
        return null;
    }

    @Override
    public String getCurrentLoginRemoteAddr() {
        return null;
    }

    @Override
    public Date getPreviousLoginDate() {
        return null;
    }

    @Override
    public String getPreviousLoginRemoteAddr() {
        return null;
    }

    @Override
    public int getLoginCount() {
        return 0;
    }

    @Override
    public UserType getUserType() {
        return UserType.USER;
    }

    @Override
    public String getCurrentWorkspaceId() {
        return this.currentWorkspaceId;
    }

    @Override
    public JSONObject getUiPreferences() {
        return preferences;
    }

    @Override
    public String getPasswordResetToken() {
        return null;
    }

    @Override
    public Date getPasswordResetTokenExpirationDate() {
        return null;
    }

    @Override
    public <PROP_TYPE> PROP_TYPE getProperty(String propertyName) {
        return getProperty(DEFAULT_KEY, propertyName);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <PROP_TYPE> PROP_TYPE getProperty(String key, String propertyName) {
        if (properties.containsKey(propertyName)) {
            return (PROP_TYPE) properties.get(propertyName).get(key);
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <PROP_TYPE> Map<String, PROP_TYPE> getProperties(String propertyName) {
        return (Map<String, PROP_TYPE>) properties.get(propertyName);
    }

    @Override
    public Map<String, Map<String, Object>> getCustomProperties() {
        return ImmutableMap.copyOf(properties);
    }

    public void setProperty(String propertyName, Object value) {
        setProperty(DEFAULT_KEY, propertyName, value);
    }

    public void setProperty(String key, String propertyName, Object value) {
        Map<String, Object> propertyValues = properties.computeIfAbsent(propertyName, (missingKey) -> new HashMap<>());
        propertyValues.put(key, value);
    }

    public void removeProperty(String key, String propertyName) {
        if (this.properties.containsKey(propertyName)) {
            this.properties.get(propertyName).remove(key);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        InMemoryUser that = (InMemoryUser) o;

        return userId.equals(that.userId);
    }

    @Override
    public int hashCode() {
        return userId.hashCode();
    }

    @Override
    public String toString() {
        return "InMemoryUser{" +
                "userId='" + userId + '\'' +
                '}';
    }
}
