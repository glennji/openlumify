package org.openlumify.core.model.user;

import org.openlumify.core.user.User;

public class UserNameAuthorizationContext extends AuthorizationContext {
    private final String username;

    public UserNameAuthorizationContext(String username, String remoteAddr) {
        super(remoteAddr);
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
