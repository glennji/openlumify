package org.openlumify.web.changePassword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Required;
import org.openlumify.core.model.user.UserRepository;
import org.openlumify.core.user.User;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;
import org.openlumify.web.BadRequestException;
import org.openlumify.web.OpenLumifyResponse;
import org.openlumify.web.clientapi.model.ClientApiSuccess;

@Singleton
public class ChangePassword implements ParameterizedHandler {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(ChangePassword.class);
    private static final String CURRENT_PASSWORD_PARAMETER_NAME = "currentPassword";
    private static final String NEW_PASSWORD_PARAMETER_NAME = "newPassword";
    private static final String NEW_PASSWORD_CONFIRMATION_PARAMETER_NAME = "newPasswordConfirmation";
    private final UserRepository userRepository;

    @Inject
    public ChangePassword(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Handle
    public ClientApiSuccess handle(
            User user,
            @Required(name = CURRENT_PASSWORD_PARAMETER_NAME) String currentPassword,
            @Required(name = NEW_PASSWORD_PARAMETER_NAME) String newPassword,
            @Required(name = NEW_PASSWORD_CONFIRMATION_PARAMETER_NAME) String newPasswordConfirmation
    ) throws Exception {
        if (userRepository.isPasswordValid(user, currentPassword)) {
            if (newPassword.length() > 0) {
                if (newPassword.equals(newPasswordConfirmation)) {
                    userRepository.setPassword(user, newPassword);
                    LOGGER.info("changed password for user: %s", user.getUsername());
                    return OpenLumifyResponse.SUCCESS;
                } else {
                    throw new BadRequestException(NEW_PASSWORD_CONFIRMATION_PARAMETER_NAME, "new password and new password confirmation do not match");
                }
            } else {
                throw new BadRequestException(NEW_PASSWORD_PARAMETER_NAME, "new password may not be blank");
            }
        } else {
            LOGGER.warn("failed to change password for user: %s due to incorrect current password", user.getUsername());
            throw new BadRequestException(CURRENT_PASSWORD_PARAMETER_NAME, "incorrect current password");
        }
    }
}
