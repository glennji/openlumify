package org.visallo.web.auth;

import org.apache.commons.lang.StringUtils;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.CurrentUser;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.visallo.core.config.Configuration.AUTH_TOKEN_EXPIRATION_IN_MINS;

public class AuthTokenFilter implements Filter {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(AuthTokenFilter.class);
    private static final int MIN_AUTH_TOKEN_EXPIRATION_MINS = 1;
    public static final String TOKEN_COOKIE_NAME = "JWT";
    public static final String TOKEN_HTTP_HEADER_NAME = "Authorization";
    public static final String TOKEN_HTTP_HEADER_TYPE = "Bearer";

    private long tokenValidityDurationInMinutes;
    private UserRepository userRepository;
    private AuthTokenRepository authTokenRepository;

    @Override
    public void init(FilterConfig filterConfig) {
        tokenValidityDurationInMinutes = Long.parseLong(
                getRequiredInitParameter(filterConfig, AUTH_TOKEN_EXPIRATION_IN_MINS)
        );
        if (tokenValidityDurationInMinutes < MIN_AUTH_TOKEN_EXPIRATION_MINS) {
            throw new VisalloException("Configuration: " +
                    "'" + AUTH_TOKEN_EXPIRATION_IN_MINS + "' " +
                    "must be at least " + MIN_AUTH_TOKEN_EXPIRATION_MINS + " minute(s)"
            );
        }

        userRepository = InjectHelper.getInstance(UserRepository.class);
        authTokenRepository = InjectHelper.getInstance(AuthTokenRepository.class);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        doFilter((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse, filterChain);
    }

    public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        AuthToken token = getAuthToken(request);
        AuthTokenHttpResponse authTokenResponse = new AuthTokenHttpResponse(token, request, response, authTokenRepository, tokenValidityDurationInMinutes);

        CurrentUser.unset(request);
        if (token != null) {
            checkNotNull(token.getUserId(), "Auth token must contain a valid userId");
            User user = userRepository.findById(token.getUserId());
            if (user != null && authTokenRepository.isValid(user, token)) {
                CurrentUser.set(request, user, token);
                if (token.getUsage() != AuthTokenUse.WEB) {
                    chain.doFilter(request, response);
                    return;
                }
            } else {
                LOGGER.debug("User %s presented an invalid auth token: %s", user == null ? null : user.getUserId(), token.getTokenId());
                authTokenResponse.invalidateAuthentication();
                if (!token.isVerified()) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
            }
        }

        chain.doFilter(request, authTokenResponse);
    }

    @Override
    public void destroy() {

    }

    private AuthToken getAuthToken(HttpServletRequest request) {
        try {
            Cookie tokenCookie = getTokenCookie(request);
            if (tokenCookie != null) {
                AuthToken authToken = authTokenRepository.parse(tokenCookie.getValue());
                if (authToken.getUsage() == AuthTokenUse.WEB) {
                    return authToken;
                } else {
                    LOGGER.warn("Non web token passed as a cookie.");
                }
            }

            String authHeader = getTokenHeader(request);
            if (authHeader != null) {
                AuthToken authToken = authTokenRepository.parse(authHeader);
                if (authToken.getUsage() == AuthTokenUse.API) {
                    return authToken;
                } else {
                    LOGGER.warn("Non API token passed as request header.");
                }
            }
        } catch (AuthTokenException ate) {
            LOGGER.warn("Failed to parse auth token ", ate);
        }
        return null;
    }

    private String getTokenHeader(HttpServletRequest request) {
        Enumeration<String> headers = request.getHeaders(TOKEN_HTTP_HEADER_NAME);
        while (headers != null && headers.hasMoreElements()) {
            String value = headers.nextElement();
            if (value.toLowerCase().startsWith(TOKEN_HTTP_HEADER_TYPE.toLowerCase())) {
                String authHeaderValue = value.substring(TOKEN_HTTP_HEADER_TYPE.length()).trim();
                int commaIndex = authHeaderValue.indexOf(',');
                if (commaIndex > 0) {
                    authHeaderValue = authHeaderValue.substring(0, commaIndex);
                }
                return authHeaderValue;
            }
        }

        return null;
    }

    private Cookie getTokenCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        return Arrays.stream(cookies)
                .filter(cookie -> cookie.getName().equals(TOKEN_COOKIE_NAME) && !StringUtils.isEmpty(cookie.getValue()))
                .findFirst()
                .orElse(null);
    }

    private String getRequiredInitParameter(FilterConfig filterConfig, String parameterName) {
        String parameter = filterConfig.getInitParameter(parameterName);
        checkNotNull(parameter, "FilterConfig init parameter '" + parameterName + "' was not set.");
        return parameter;
    }
}
