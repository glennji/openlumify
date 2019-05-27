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
public class UserAddAuthorization implements ParameterizedHandler {
    private final AuthorizationRepository authorizationRepository;
    private final UserRepository userRepository;
    private static final String SEPARATOR = ",";

    @Inject
    public UserAddAuthorization(
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
            throw new OpenLumifyResourceNotFoundException("User " + userName + " not found");
        }

        if (!(authorizationRepository instanceof UpdatableAuthorizationRepository)) {
            throw new OpenLumifyAccessDeniedException("Authorization repository does not support updating", authUser, userName);
        }

        for (String authStr : auth.split(SEPARATOR)) {
            ((UpdatableAuthorizationRepository) authorizationRepository).addAuthorization(user, authStr, authUser);
        }

        return userRepository.toJsonWithAuths(user);
    }
}
