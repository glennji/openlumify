package org.openlumify.core.model.user;

import org.openlumify.core.model.properties.OpenLumifyProperties;
import org.openlumify.core.model.properties.types.*;

public final class UserOpenLumifyProperties {
    public static final StringSingleValueOpenLumifyProperty USERNAME = new StringSingleValueOpenLumifyProperty("http://openlumify.org/user#username");
    public static final StringSingleValueOpenLumifyProperty DISPLAY_NAME = new StringSingleValueOpenLumifyProperty("http://openlumify.org/user#displayName");
    public static final StringSingleValueOpenLumifyProperty EMAIL_ADDRESS = new StringSingleValueOpenLumifyProperty("http://openlumify.org/user#emailAddress");
    public static final DateSingleValueOpenLumifyProperty CREATE_DATE = new DateSingleValueOpenLumifyProperty("http://openlumify.org/user#createDate");
    public static final DateSingleValueOpenLumifyProperty CURRENT_LOGIN_DATE = new DateSingleValueOpenLumifyProperty("http://openlumify.org/user#currentLoginDate");
    public static final StringSingleValueOpenLumifyProperty CURRENT_LOGIN_REMOTE_ADDR = new StringSingleValueOpenLumifyProperty("http://openlumify.org/user#currentLoginRemoteAddr");
    public static final DateSingleValueOpenLumifyProperty PREVIOUS_LOGIN_DATE = new DateSingleValueOpenLumifyProperty("http://openlumify.org/user#previousLoginDate");
    public static final StringSingleValueOpenLumifyProperty PREVIOUS_LOGIN_REMOTE_ADDR = new StringSingleValueOpenLumifyProperty("http://openlumify.org/user#previousLoginRemoteAddr");
    public static final IntegerSingleValueOpenLumifyProperty LOGIN_COUNT = new IntegerSingleValueOpenLumifyProperty("http://openlumify.org/user#loginCount");
    public static final StringSingleValueOpenLumifyProperty CURRENT_WORKSPACE = new StringSingleValueOpenLumifyProperty("http://openlumify.org/user#currentWorkspace");
    public static final JsonSingleValueOpenLumifyProperty UI_PREFERENCES = new JsonSingleValueOpenLumifyProperty("http://openlumify.org/user#uiPreferences");
    public static final ByteArraySingleValueOpenLumifyProperty PASSWORD_SALT = new ByteArraySingleValueOpenLumifyProperty("http://openlumify.org/user#passwordSalt");
    public static final ByteArraySingleValueOpenLumifyProperty PASSWORD_HASH = new ByteArraySingleValueOpenLumifyProperty("http://openlumify.org/user#passwordHash");
    public static final StringSingleValueOpenLumifyProperty PASSWORD_RESET_TOKEN = new StringSingleValueOpenLumifyProperty("http://openlumify.org/user#passwordResetToken");
    public static final DateSingleValueOpenLumifyProperty PASSWORD_RESET_TOKEN_EXPIRATION_DATE = new DateSingleValueOpenLumifyProperty("http://openlumify.org/user#passwordResetTokenExpirationDate");

    public static boolean isBuiltInProperty(String propertyName) {
        return OpenLumifyProperties.isBuiltInProperty(propertyName)
                || OpenLumifyProperties.isBuiltInProperty(UserOpenLumifyProperties.class, propertyName);
    }
}
