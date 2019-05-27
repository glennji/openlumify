package org.openlumify.web.plugin.adminUserTools.userPropertyAuth;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Required;
import org.json.JSONObject;
import org.openlumify.core.exception.OpenLumifyAccessDeniedException;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.model.user.AuthorizationRepository;
import org.openlumify.core.model.user.UpdatableAuthorizationRepository;
import org.openlumify.core.model.user.UserRepository;
import org.openlumify.core.user.User;

@Singleton
public class UserRemoveAuthorization implements ParameterizedHandler {
    private final UserRepository userRepository;
    private final AuthorizationRepository authorizationRepository;

    @Inject
    public UserRemoveAuthorization(
            UserRepository userRepository,
            AuthorizationRepository authorizationRepository
    ) {
        this.userRepository = userRepository;
        this.authorizationRepository = authorizationRepository;
    }

    @Handle
    public JSONObject handle(
            @Required(name = "user-name") String userName,
            @Required(name = "auth") String auth,
            User authUser
    ) throws Exception {
        User user = userRepository.findByUsername(userName);
        if (user == null) {
            throw new OpenLumifyResourceNotFoundException("Could not find user: " + userName);
        }

        if (!(authorizationRepository instanceof UpdatableAuthorizationRepository)) {
            throw new OpenLumifyAccessDeniedException("Authorization repository does not support updating", authUser, userName);
        }

        ((UpdatableAuthorizationRepository) authorizationRepository).removeAuthorization(user, auth, authUser);
        return userRepository.toJsonWithAuths(user);
    }
}
