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

    <PROP_TYPE> PROP_TYPE getProperty(String propertyName);

    <PROP_TYPE> PROP_TYPE getProperty(String key, String propertyName);

    <PROP_TYPE> Map<String, PROP_TYPE> getProperties(String propertyName);

    Map<String, Map<String, Object>> getCustomProperties();
}
