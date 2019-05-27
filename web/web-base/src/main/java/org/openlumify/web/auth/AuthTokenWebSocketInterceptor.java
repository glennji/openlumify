package org.openlumify.web.auth;

import org.apache.commons.lang.StringUtils;
import org.atmosphere.cpr.*;
import org.openlumify.core.bootstrap.InjectHelper;
import org.openlumify.core.config.Configuration;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.model.user.UserRepository;
import org.openlumify.core.user.User;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;
import org.openlumify.web.CurrentUser;

import javax.crypto.SecretKey;
import javax.servlet.http.HttpServletRequest;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.openlumify.core.config.Configuration.AUTH_TOKEN_PASSWORD;
import static org.openlumify.core.config.Configuration.AUTH_TOKEN_SALT;

public class AuthTokenWebSocketInterceptor implements AtmosphereInterceptor {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(AuthTokenWebSocketInterceptor.class);

    private SecretKey tokenSigningKey;
    private int tokenExpirationToleranceInSeconds;
    private UserRepository userRepository;

    @Override
    public void configure(AtmosphereConfig config) {
        String keyPassword = config.getInitParameter(AUTH_TOKEN_PASSWORD);
        checkNotNull(keyPassword, "AtmosphereConfig init parameter '" + AUTH_TOKEN_PASSWORD + "' was not set.");
        String keySalt = config.getInitParameter(AUTH_TOKEN_SALT);
        checkNotNull(keySalt, "AtmosphereConfig init parameter '" + AUTH_TOKEN_SALT + "' was not set.");
        tokenExpirationToleranceInSeconds = config.getInitParameter(Configuration.AUTH_TOKEN_EXPIRATION_TOLERANCE_IN_SECS, 0);
        userRepository = InjectHelper.getInstance(UserRepository.class);

        try {
            tokenSigningKey = AuthToken.generateKey(keyPassword, keySalt);
        } catch (Exception e) {
            throw new OpenLumifyException("Key generation failed", e);
        }
    }

    @Override
    public Action inspect(AtmosphereResource resource) {
        try {
            AtmosphereRequest request = resource.getRequest();
            AuthToken token = getAuthToken(request);

            if (token != null && !token.isExpired(tokenExpirationToleranceInSeconds)) {
                setCurrentUser(request, token);
            }
        } catch (AuthTokenException e) {
            LOGGER.warn("Auth token signature verification failed", e);
            return Action.CANCELLED;
        }

        return Action.CONTINUE;
    }

    @Override
    public void postInspect(AtmosphereResource resource) {
        // noop
    }

    @Override
    public void destroy() {
        // noop
    }

    private AuthToken getAuthToken(AtmosphereRequest request) throws AuthTokenException {
        String cookieString = request.getHeader("cookie");

        if (cookieString != null) {
            int tokenCookieIndex = cookieString.indexOf(AuthTokenFilter.TOKEN_COOKIE_NAME);
            if (tokenCookieIndex > -1) {
                int equalsSeperatorIndex = cookieString.indexOf("=", tokenCookieIndex);
                int cookieSeparatorIndex = cookieString.indexOf(";", equalsSeperatorIndex);
                if (cookieSeparatorIndex < 0) {
                    cookieSeparatorIndex = cookieString.length();
                }
                String tokenString = cookieString.substring(equalsSeperatorIndex + 1, cookieSeparatorIndex).trim();
                if (!StringUtils.isEmpty(tokenString)) {
                    return AuthToken.parse(tokenString, tokenSigningKey);
                }
            }
        }

        return null;
    }

    private void setCurrentUser(HttpServletRequest request, AuthToken token) {
        checkNotNull(token.getUserId(), "Auth token did not contain the userId");
        User user = userRepository.findById(token.getUserId());
        CurrentUser.set(request, user);
    }
}
