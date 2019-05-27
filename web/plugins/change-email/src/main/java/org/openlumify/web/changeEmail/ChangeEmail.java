package org.openlumify.web.changeEmail;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Required;
import org.openlumify.core.model.user.UserRepository;
import org.openlumify.core.user.User;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;
import org.openlumify.web.OpenLumifyResponse;
import org.openlumify.web.clientapi.model.ClientApiSuccess;

@Singleton
public class ChangeEmail implements ParameterizedHandler {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(ChangeEmail.class);
    private static final String EMAIL_PARAMETER_NAME = "email";
    private final UserRepository userRepository;

    @Inject
    public ChangeEmail(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Handle
    public ClientApiSuccess handle(
            User user,
            @Required(name = EMAIL_PARAMETER_NAME) String email
    ) throws Exception {
        userRepository.setEmailAddress(user, email);
        LOGGER.info("changed email for user: %s", user.getUsername());
        return OpenLumifyResponse.SUCCESS;
    }
}
