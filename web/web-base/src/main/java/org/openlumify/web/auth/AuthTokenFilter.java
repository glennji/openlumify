package org.openlumify.web.auth;

import org.apache.commons.lang.StringUtils;
import org.openlumify.core.bootstrap.InjectHelper;
import org.openlumify.core.config.Configuration;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.model.user.UserRepository;
import org.openlumify.core.user.User;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;
import org.openlumify.web.CurrentUser;

import javax.crypto.SecretKey;
import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.openlumify.core.config.Configuration.*;

public class AuthTokenFilter implements Filter {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(AuthTokenFilter.class);
    private static final int MIN_AUTH_TOKEN_EXPIRATION_MINS = 1;
    public static final String TOKEN_COOKIE_NAME = "JWT";

    private SecretKey tokenSigningKey;
    private long tokenValidityDurationInMinutes;
    private int tokenExpirationToleranceInSeconds;
    private UserRepository userRepository;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        tokenValidityDurationInMinutes = Long.parseLong(
                getRequiredInitParameter(filterConfig, AUTH_TOKEN_EXPIRATION_IN_MINS)
        );
        if (tokenValidityDurationInMinutes < MIN_AUTH_TOKEN_EXPIRATION_MINS) {
            throw new OpenLumifyException("Configuration: " +
                "'" +  AUTH_TOKEN_EXPIRATION_IN_MINS + "' " +
                "must be at least " + MIN_AUTH_TOKEN_EXPIRATION_MINS + " minute(s)"
            );
        }

        tokenExpirationToleranceInSeconds = Integer.parseInt(
                getRequiredInitParameter(filterConfig, Configuration.AUTH_TOKEN_EXPIRATION_TOLERANCE_IN_SECS)
        );

        String keyPassword = getRequiredInitParameter(filterConfig, AUTH_TOKEN_PASSWORD);
        String keySalt = getRequiredInitParameter(filterConfig, AUTH_TOKEN_SALT);
        userRepository = InjectHelper.getInstance(UserRepository.class);

        try {
            tokenSigningKey = AuthToken.generateKey(keyPassword, keySalt);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        doFilter((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse, filterChain);
    }

    public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            AuthToken token = getAuthToken(request);
            AuthTokenHttpResponse authTokenResponse = new AuthTokenHttpResponse(token, request, response, tokenSigningKey, tokenValidityDurationInMinutes);

            if (token != null) {
                if (token.isExpired(tokenExpirationToleranceInSeconds)) {
                    authTokenResponse.invalidateAuthentication();
                } else {
                    User user = userRepository.findById(token.getUserId());
                    if (user != null) {
                        CurrentUser.set(request, user);
                    } else {
                        authTokenResponse.invalidateAuthentication();
                    }
                }
            }

            chain.doFilter(request, authTokenResponse);
        } catch (AuthTokenException ex) {
            LOGGER.warn("Auth token signature verification failed", ex);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    @Override
    public void destroy() {

    }

    private AuthToken getAuthToken(HttpServletRequest request) throws AuthTokenException {
        Cookie tokenCookie = getTokenCookie(request);
        return tokenCookie != null ? AuthToken.parse(tokenCookie.getValue(), tokenSigningKey) : null;
    }

    private Cookie getTokenCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        Cookie found = null;

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(AuthTokenFilter.TOKEN_COOKIE_NAME)) {
                if (StringUtils.isEmpty(cookie.getValue())) {
                    return null;
                } else {
                    found = cookie;
                }
            }
        }

        return found;
    }

    private String getRequiredInitParameter(FilterConfig filterConfig, String parameterName) {
        String parameter = filterConfig.getInitParameter(parameterName);
        checkNotNull(parameter, "FilterConfig init parameter '" + parameterName + "' was not set.");
        return parameter;
    }
}
