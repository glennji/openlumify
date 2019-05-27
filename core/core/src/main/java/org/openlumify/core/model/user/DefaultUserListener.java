package org.openlumify.core.model.user;

import org.openlumify.core.user.User;

import java.util.Set;

public class DefaultUserListener implements UserListener {
    @Override
    public void newUserAdded(User user) {

    }

    @Override
    public void userDeleted(User user) {

    }

    @Override
    public void userPrivilegesUpdated(User user, Set<String> privileges) {

    }

    @Override
    public void userRemoveAuthorization(User user, String auth) {

    }

    @Override
    public void userAddAuthorization(User user, String auth) {

    }

    @Override
    public void userLogin(User user, AuthorizationContext authorizationContext) {

    }
}
