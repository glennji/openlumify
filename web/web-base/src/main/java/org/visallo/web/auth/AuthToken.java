package org.visallo.web.auth;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;

public class AuthToken {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final String userId;
    private final Date expiration;
    private final String tokenId;
    private final String description;
    private final boolean verified;
    private final AuthTokenUse usage;

    public AuthToken(String userId, Date expiration, boolean verified, AuthTokenUse usage) {
        this(AuthToken.generateTokenId(), userId, expiration, verified, null, usage);
    }

    public AuthToken(String userId, Date expiration, boolean verified, String description, AuthTokenUse usage) {
        this(AuthToken.generateTokenId(), userId, expiration, verified, description, usage);
    }

    AuthToken(String tokenId, String userId, Date expiration, boolean verified, String description, AuthTokenUse usage) {
        this.tokenId = tokenId;
        this.userId = userId;
        this.expiration = expiration;
        this.verified = verified;
        this.description = description;
        this.usage = usage;
    }

    public String getTokenId() {
        return tokenId;
    }

    public String getUserId() {
        return userId;
    }

    public Date getExpiration() {
        return expiration;
    }

    public AuthTokenUse getUsage() {
        return usage;
    }

    public String getDescription() {
        return description;
    }

    public boolean isVerified() {
        return verified;
    }

    public boolean isExpired(int toleranceInSeconds) {
        Calendar expirationWithTolerance = Calendar.getInstance();
        expirationWithTolerance.setTime(expiration);
        expirationWithTolerance.add(Calendar.SECOND, toleranceInSeconds);
        return expirationWithTolerance.getTime().before(new Date());
    }

    private static String generateTokenId() {
        byte[] randomBytes = new byte[128];
        SECURE_RANDOM.nextBytes(randomBytes);

        ByteBuffer currentTimeBuffer = ByteBuffer.allocate(Long.BYTES);
        currentTimeBuffer.putLong(System.currentTimeMillis());

        return Base64.getEncoder().encodeToString(randomBytes)
                + "@" + Base64.getEncoder().encodeToString(currentTimeBuffer.array());
    }
}
