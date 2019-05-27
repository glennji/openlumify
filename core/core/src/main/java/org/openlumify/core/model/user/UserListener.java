package org.openlumify.core.model.user;

import org.openlumify.core.user.User;

import java.util.Set;

public interface UserListener {
    void newUserAdded(User user);

    void userDeleted(User user);

    void userPrivilegesUpdated(User user, Set<String> privileges);

    void userRemoveAuthorization(User user, String auth);

    void userAddAuthorization(User user, String auth);

    void userLogin(User user, AuthorizationContext authorizationContext);
}
