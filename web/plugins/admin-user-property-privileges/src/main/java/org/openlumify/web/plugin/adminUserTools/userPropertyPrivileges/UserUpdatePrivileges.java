package org.openlumify.web.plugin.adminUserTools.userPropertyPrivileges;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Required;
import org.json.JSONObject;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.model.user.PrivilegeRepository;
import org.openlumify.core.model.user.UserPropertyPrivilegeRepository;
import org.openlumify.core.model.user.UserRepository;
import org.openlumify.core.user.User;
import org.openlumify.web.clientapi.model.Privilege;

import java.util.Set;

@Singleton
public class UserUpdatePrivileges implements ParameterizedHandler {
    private final UserRepository userRepository;
    private final UserPropertyPrivilegeRepository privilegeRepository;

    @Inject
    public UserUpdatePrivileges(UserRepository userRepository, PrivilegeRepository privilegeRepository) {
        if (!(privilegeRepository instanceof UserPropertyPrivilegeRepository)) {
            throw new OpenLumifyException(UserPropertyPrivilegeRepository.class.getName() + " required");
        }

        this.userRepository = userRepository;
        this.privilegeRepository = (UserPropertyPrivilegeRepository) privilegeRepository;
    }

    @Handle
    public JSONObject handle(
            @Required(name = "user-name") String userName,
            @Required(name = "privileges") String privilegesParameter,
            User authUser
    ) throws Exception {
        Set<String> privileges = Privilege.stringToPrivileges(privilegesParameter);

        User user = userRepository.findByUsername(userName);
        if (user == null) {
            throw new OpenLumifyResourceNotFoundException("Could not find user: " + userName);
        }

        privilegeRepository.setPrivileges(user, privileges, authUser);

        return userRepository.toJsonWithAuths(user);
    }
}
