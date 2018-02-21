package org.visallo.core.user;

import org.json.JSONObject;
import org.vertexium.mutation.ElementMutation;
import org.visallo.web.clientapi.model.UserType;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

public interface User extends Serializable {
    long serialVersionUID = 2L;

    String DEFAULT_KEY = ElementMutation.DEFAULT_KEY;

    String getUserId();

    String getUsername();

    String getDisplayName();

    String getEmailAddress();

    Date getCreateDate();

    Date getCurrentLoginDate();

    String getCurrentLoginRemoteAddr();

    Date getPreviousLoginDate();

    String getPreviousLoginRemoteAddr();

    int getLoginCount();

    UserType getUserType();

    String getCurrentWorkspaceId();

    JSONObject getUiPreferences();

    String getPasswordResetToken();

    Date getPasswordResetTokenExpirationDate();

    <T> T getProperty(String propertyName);

    <T> T getProperty(String key, String propertyName);

    <T> Map<String, T> getProperties(String propertyName);

    Map<String, Map<String, Object>> getCustomProperties();
}
