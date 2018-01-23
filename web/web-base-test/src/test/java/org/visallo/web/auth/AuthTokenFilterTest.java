package org.visallo.web.auth;

import com.google.inject.Injector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.config.Configuration;
import org.visallo.core.config.ConfigurationLoader;
import org.visallo.core.config.HashMapConfigurationLoader;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.user.UserVisalloProperties;
import org.visallo.vertexium.model.user.InMemoryUser;
import org.visallo.web.CurrentUser;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.visallo.core.config.Configuration.*;
import static org.visallo.web.auth.AuthTokenFilter.TOKEN_HTTP_HEADER_NAME;
import static org.visallo.web.auth.AuthTokenFilter.TOKEN_HTTP_HEADER_TYPE;
import static org.visallo.web.auth.AuthTokenUse.API;
import static org.visallo.web.auth.AuthTokenUse.WEB;

@RunWith(MockitoJUnitRunner.class)
public class AuthTokenFilterTest {

    private static final int EXPIRATION = 60;
    private static final int EXPIRATION_TOLERANCE = 5;
    private static final String PASSWORD = "password";
    private static final String SALT = "salt";

    @Mock
    private FilterConfig filterConfig;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Injector injector;

    private AuthTokenFilter filter;

    private InMemoryUser user = new InMemoryUser("user123");

    private AuthTokenRepository authTokenRepository;

    @Before
    public void before() {
        Map<String, String> config = new HashMap<>(Configuration.DEFAULTS);
        config.put(AUTH_TOKEN_PASSWORD, PASSWORD);
        config.put(AUTH_TOKEN_SALT, SALT);
        config.put(AUTH_TOKEN_EXPIRATION_IN_MINS, Integer.toString(EXPIRATION));
        config.put(AUTH_TOKEN_EXPIRATION_TOLERANCE_IN_SECS, Integer.toString(EXPIRATION_TOLERANCE));
        Configuration configuration = ConfigurationLoader.load(HashMapConfigurationLoader.class, config);

        authTokenRepository = new AuthTokenRepository(configuration, userRepository);

        when(filterConfig.getInitParameter(AUTH_TOKEN_EXPIRATION_IN_MINS)).thenReturn(Integer.toString(EXPIRATION));
        when(injector.getInstance(UserRepository.class)).thenReturn(userRepository);
        when(injector.getInstance(AuthTokenRepository.class)).thenReturn(authTokenRepository);
        InjectHelper.setInjector(injector);
        filter = new AuthTokenFilter();
        filter.init(filterConfig);

        when(userRepository.findById(user.getUserId())).thenReturn(user);
    }

    @Test
    public void testNoTokenCookiePresentDoesNotSetToken() throws IOException, ServletException {
        when(request.getCookies()).thenReturn(new Cookie[0]);
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(eq(request), any(HttpServletResponse.class));
        verify(response, never()).addCookie(any(Cookie.class));
    }

    @Test
    public void testValidInboundWebTokenSetsCurrentUser() throws Exception {
        AuthToken token = getToken(user.getUserId(), WEB);
        Cookie cookie = getTokenCookie(token);
        when(request.getCookies()).thenReturn(new Cookie[]{cookie});
        filter.doFilter(request, response, chain);
        verify(request).setAttribute(CurrentUser.CURRENT_USER_REQ_ATTR_NAME, user);
        verify(chain).doFilter(eq(request), any(HttpServletResponse.class));
    }

    @Test
    public void testValidInboundWebTokenForUnknownUserDoesNotSetCurrentUser() throws Exception {
        AuthToken token = getToken(user.getUserId(), WEB);
        Cookie cookie = getTokenCookie(token);
        when(request.getCookies()).thenReturn(new Cookie[]{cookie});
        when(userRepository.findById(user.getUserId())).thenReturn(null);
        filter.doFilter(request, response, chain);
        verify(request, never()).setAttribute(CurrentUser.CURRENT_USER_REQ_ATTR_NAME, user);
        verify(chain).doFilter(eq(request), any(HttpServletResponse.class));
    }

    @Test
    public void testValidInboundWebTokenAsHeaderDoesNotSetCurrentUser() throws Exception {
        AuthToken token = getToken(user.getUserId(), WEB);
        when(request.getCookies()).thenReturn(new Cookie[]{});
        when(request.getHeaders(TOKEN_HTTP_HEADER_NAME)).thenReturn(Collections.enumeration(Collections.singleton(getTokenHeader(token))));
        filter.doFilter(request, response, chain);
        verify(request, never()).setAttribute(CurrentUser.CURRENT_USER_REQ_ATTR_NAME, user);
        verify(chain).doFilter(eq(request), any(HttpServletResponse.class));
    }

    @Test
    public void testValidInboundApiTokenAsHeaderSetsCurrentUser() throws Exception {
        AuthToken token = getToken(user.getUserId(), API);
        when(request.getCookies()).thenReturn(new Cookie[]{});
        when(request.getHeaders(TOKEN_HTTP_HEADER_NAME)).thenReturn(Collections.enumeration(Collections.singleton(getTokenHeader(token))));
        filter.doFilter(request, response, chain);
        verify(request).setAttribute(CurrentUser.CURRENT_USER_REQ_ATTR_NAME, user);
        verify(chain).doFilter(eq(request), any(HttpServletResponse.class));
    }

    @Test
    public void testValidInboundApiTokenAsCookieDoesNotSetCurrentUser() throws Exception {
        AuthToken token = getToken(user.getUserId(), API);
        Cookie cookie = getTokenCookie(token);
        when(request.getCookies()).thenReturn(new Cookie[]{cookie});
        filter.doFilter(request, response, chain);
        verify(request, never()).setAttribute(CurrentUser.CURRENT_USER_REQ_ATTR_NAME, user);
        verify(chain).doFilter(eq(request), any(HttpServletResponse.class));
    }

    @Test
    public void testRevokedInboundApiTokenAsHeaderDoesNotSetCurrentUser() throws Exception {
        AuthToken token = getToken(user.getUserId(), API);
        user.removeProperty("junit", UserVisalloProperties.API_TOKEN.getPropertyName());
        when(request.getCookies()).thenReturn(new Cookie[]{});
        when(request.getHeaders(TOKEN_HTTP_HEADER_NAME)).thenReturn(Collections.enumeration(Collections.singleton(getTokenHeader(token))));
        filter.doFilter(request, response, chain);
        verify(request, never()).setAttribute(CurrentUser.CURRENT_USER_REQ_ATTR_NAME, user);
        verify(chain).doFilter(eq(request), any(HttpServletResponse.class));
    }

    @Test
    public void testExpiredTokenDoesNotSetCurrentUser() throws Exception {
        AuthToken token = getToken(user.getUserId(), new Date(System.currentTimeMillis() - (EXPIRATION_TOLERANCE * 1000) - 1), WEB);
        Cookie cookie = getTokenCookie(token);
        when(request.getCookies()).thenReturn(new Cookie[]{cookie});
        filter.doFilter(request, response, chain);
        verify(request, never()).setAttribute(CurrentUser.CURRENT_USER_REQ_ATTR_NAME, user);
        verify(chain).doFilter(eq(request), any(HttpServletResponse.class));
    }

    @Test
    public void testExpiredTokenWithinToleranceDoesSetCurrentUser() throws Exception {
        AuthToken token = getToken(user.getUserId(), new Date(System.currentTimeMillis() - (EXPIRATION_TOLERANCE * 1000) / 2), WEB);
        Cookie cookie = getTokenCookie(token);
        when(request.getCookies()).thenReturn(new Cookie[]{cookie});
        filter.doFilter(request, response, chain);
        verify(request).setAttribute(CurrentUser.CURRENT_USER_REQ_ATTR_NAME, user);
        verify(chain).doFilter(eq(request), any(HttpServletResponse.class));
    }

    @Test
    public void testExpiredTokenRemovesTokenCookie() throws Exception {
        AuthToken token = getToken(user.getUserId(), new Date(System.currentTimeMillis() - (EXPIRATION_TOLERANCE * 1000) - 1), WEB);
        Cookie cookie = getTokenCookie(token);
        when(request.getCookies()).thenReturn(new Cookie[]{cookie});
        when(response.isCommitted()).thenReturn(false);
        filter.doFilter(request, response, chain);
        verify(response).addCookie(argThat(matchesCookie((right, rightToken) ->
                right.getName().equals(cookie.getName())
                        && right.getMaxAge() == 0
                        && right.getValue() == null
                        && rightToken == null)));
        verify(chain).doFilter(eq(request), any(HttpServletResponse.class));
    }

    @Test
    public void testCurrentUserSetCausesTokenCookieToBeSet() throws Exception {
        when(request.getCookies()).thenReturn(new Cookie[0]);
        when(response.isCommitted()).thenReturn(false);

        FilterChain testChain = (request, response) -> {
            when(request.getAttribute(CurrentUser.CURRENT_USER_REQ_ATTR_NAME)).thenReturn(user);
            response.getWriter();
        };

        filter.doFilter(request, response, testChain);

        verify(response).addCookie(argThat(matchesCookie((cokie, token) -> token.getUserId().equals(user.getUserId()))));
    }

    @Test
    public void testTokenCookieCloseToExpirationGetsReset() throws Exception {
        AuthToken token = getToken(user.getUserId(), new Date(System.currentTimeMillis() + 60000), WEB);
        Cookie cookie = getTokenCookie(token);
        when(request.getCookies()).thenReturn(new Cookie[]{cookie});
        when(request.getAttribute(CurrentUser.CURRENT_USER_REQ_ATTR_NAME)).thenReturn(user);

        when(response.isCommitted()).thenReturn(false);

        FilterChain testChain = (request, response) -> response.getWriter();

        filter.doFilter(request, response, testChain);

        verify(response).addCookie(argThat(matchesCookie((right, rightToken) ->
                !rightToken.getTokenId().equals(token.getTokenId()) &&
                        rightToken.getUserId().equals(user.getUserId()))));
    }

    @Test
    public void testApiTokenCookieCloseToExpirationDoesNotGetReset() throws Exception {
        AuthToken token = getToken(user.getUserId(), new Date(System.currentTimeMillis() + 60000), API);
        when(request.getCookies()).thenReturn(new Cookie[]{});
        when(request.getHeaders(TOKEN_HTTP_HEADER_NAME)).thenReturn(Collections.enumeration(Collections.singleton(getTokenHeader(token))));
        when(request.getAttribute(CurrentUser.CURRENT_USER_REQ_ATTR_NAME)).thenReturn(user);

        FilterChain testChain = (request, response) -> response.getWriter();

        filter.doFilter(request, response, testChain);

        verify(response, never()).addCookie(any());
    }

    @Test
    public void testTokenSignatureFailureSendsError() throws Exception {
        AuthToken token = getToken(user.getUserId(), WEB);
        Cookie cookie = getTokenCookie(authTokenRepository.serialize(token) + "a", token.getExpiration());
        when(request.getCookies()).thenReturn(new Cookie[]{cookie});
        filter.doFilter(request, response, chain);
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
        verify(request, never()).setAttribute(CurrentUser.CURRENT_USER_REQ_ATTR_NAME, user);
        verify(chain, never()).doFilter(eq(request), any(HttpServletResponse.class));
    }

    private AuthToken getToken(String userid, AuthTokenUse use) {
        return getToken(userid, new Date(System.currentTimeMillis() + (EXPIRATION * 60 * 1000) - 1), use);
    }

    private AuthToken getToken(String userid, Date expiration, AuthTokenUse use) {
        AuthToken authToken = new AuthToken(userid, expiration, true, use);
        if (use == AuthTokenUse.API) {
            try {
                user.setProperty("junit", UserVisalloProperties.API_TOKEN.getPropertyName(), authTokenRepository.serialize(authToken));
            } catch (AuthTokenException e) {
                fail("Unable to serialize token");
            }
        }
        return authToken;
    }

    private String getTokenHeader(AuthToken token) throws AuthTokenException {
        return TOKEN_HTTP_HEADER_TYPE + " " + authTokenRepository.serialize(token);
    }

    private Cookie getTokenCookie(AuthToken token) throws AuthTokenException {
        return getTokenCookie(authTokenRepository.serialize(token), token.getExpiration());
    }

    private Cookie getTokenCookie(String value, Date expiration) {
        Cookie cookie = new Cookie(AuthTokenFilter.TOKEN_COOKIE_NAME, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setMaxAge((int) ((expiration.getTime() - System.currentTimeMillis()) / 1000));
        return cookie;
    }

    private ArgumentMatcher<Cookie> matchesCookie(BiFunction<Cookie, AuthToken, Boolean> matcher) {
        return new ArgumentMatcher<Cookie>() {
            @Override
            public boolean matches(Object c) {
                try {
                    Cookie cookie = (Cookie) c;
                    AuthToken token = cookie.getValue() == null ? null : authTokenRepository.parse(cookie.getValue());
                    return matcher.apply(cookie, token);
                } catch (Exception e) {
                    fail("token signing failed: " + e);
                }
                return false;
            }
        };
    }
}
