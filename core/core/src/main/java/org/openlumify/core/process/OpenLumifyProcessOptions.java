package org.openlumify.core.process;

import org.openlumify.core.user.User;

public class OpenLumifyProcessOptions {
    private final User user;

    public OpenLumifyProcessOptions(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
