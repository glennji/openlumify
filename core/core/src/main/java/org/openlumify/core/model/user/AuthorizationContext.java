package org.openlumify.core.model.user;

public class AuthorizationContext {
    private final String remoteAddr;

    public AuthorizationContext(String remoteAddr) {
        this.remoteAddr = remoteAddr;
    }

    public String getRemoteAddr() {
        return remoteAddr;
    }
}
