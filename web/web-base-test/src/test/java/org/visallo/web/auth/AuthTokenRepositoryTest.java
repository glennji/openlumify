package org.visallo.web.auth;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.visallo.core.config.Configuration;
import org.visallo.core.config.ConfigurationLoader;
import org.visallo.core.config.HashMapConfigurationLoader;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.user.UserVisalloProperties;
import org.visallo.vertexium.model.user.InMemoryUser;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.visallo.core.config.Configuration.*;

@RunWith(MockitoJUnitRunner.class)
public class AuthTokenRepositoryTest {
    private static final int EXPIRATION = 60;
    private static final int EXPIRATION_TOLERANCE = 5;
    private static final String PASSWORD = "password";
    private static final String SALT = "salt";

    @Mock
    private UserRepository userRepository;

    private AuthTokenRepository authTokenRepository;

    private InMemoryUser user = new InMemoryUser("user123");

    @Before
    public void before() {
        Map<String, String> config = new HashMap<>(Configuration.DEFAULTS);
        config.put(AUTH_TOKEN_PASSWORD, PASSWORD);
        config.put(AUTH_TOKEN_SALT, SALT);
        config.put(AUTH_TOKEN_EXPIRATION_IN_MINS, Integer.toString(EXPIRATION));
        config.put(AUTH_TOKEN_EXPIRATION_TOLERANCE_IN_SECS, Integer.toString(EXPIRATION_TOLERANCE));
        Configuration configuration = ConfigurationLoader.load(HashMapConfigurationLoader.class, config);

        authTokenRepository = new AuthTokenRepository(configuration, userRepository);
        when(userRepository.findById(user.getUserId())).thenReturn(user);
    }

    @Test
    public void testValidTokenCreationAndValidation() throws Exception {
        Date expiration = new Date(System.currentTimeMillis() + (EXPIRATION * 60 * 1000));
        AuthToken token = new AuthToken(user.getUserId(), expiration, true, AuthTokenUse.WEB);
        String tokenText = authTokenRepository.serialize(token);
        AuthToken parsedToken = authTokenRepository.parse(tokenText);
        assertEquals(user.getUserId(), parsedToken.getUserId());
        assertNotNull(parsedToken.getTokenId());
        assertEquals(parsedToken.getTokenId(), parsedToken.getTokenId());
        assertTrue("Dates aren't close enough to each other",
                Math.abs(expiration.getTime() - parsedToken.getExpiration().getTime()) < 1000);
        assertTrue(authTokenRepository.isValid(parsedToken));
    }

    @Test
    public void testValidApiTokenCreationAndValidation() throws Exception {
        Date expiration = new Date(System.currentTimeMillis() + (EXPIRATION * 60 * 1000));
        AuthToken token = new AuthToken(user.getUserId(), expiration, true, AuthTokenUse.API);
        String tokenText = authTokenRepository.serialize(token);
        user.setProperty("junit", UserVisalloProperties.API_TOKEN.getPropertyName(), tokenText);
        AuthToken parsedToken = authTokenRepository.parse(tokenText);
        assertEquals(user.getUserId(), parsedToken.getUserId());
        assertNotNull(parsedToken.getTokenId());
        assertEquals(parsedToken.getTokenId(), parsedToken.getTokenId());
        assertTrue("Dates aren't close enough to each other",
                Math.abs(expiration.getTime() - parsedToken.getExpiration().getTime()) < 1000);
        assertTrue(authTokenRepository.isValid(parsedToken));
    }

    @Test
    public void testRevokedApiTokenIsNotValid() throws Exception {
        Date expiration = new Date(System.currentTimeMillis() + (EXPIRATION * 60 * 1000));
        AuthToken token = new AuthToken(user.getUserId(), expiration, true, AuthTokenUse.API);
        String tokenText = authTokenRepository.serialize(token);
        AuthToken parsedToken = authTokenRepository.parse(tokenText);
        assertFalse(authTokenRepository.isValid(parsedToken));
    }

    @Test
    public void testTokenReportsExpirationCorrectly() throws Exception {
        Date expiration = new Date(System.currentTimeMillis() - 10);
        AuthToken token = new AuthToken("userid", expiration, true, AuthTokenUse.WEB);
        String tokenText = authTokenRepository.serialize(token);
        AuthToken parsedToken = authTokenRepository.parse(tokenText);
        assertFalse(parsedToken.isExpired(60));
        assertTrue(parsedToken.isExpired(0));
    }

    @Test
    public void testTokenSignatureVerificationFailure() throws Exception {
        Date expiration = new Date(System.currentTimeMillis() + (EXPIRATION * 60 * 1000));
        String userid = "userid";
        AuthToken token = new AuthToken(userid, expiration, true, AuthTokenUse.WEB);
        String tokenText = authTokenRepository.serialize(token);

        AuthToken parsedToken = authTokenRepository.parse(tokenText + "a");
        assertFalse(parsedToken.isVerified());
        assertFalse(parsedToken.isExpired(60));
        assertEquals(userid, parsedToken.getUserId());
        assertFalse(authTokenRepository.isValid(parsedToken));
    }
}
