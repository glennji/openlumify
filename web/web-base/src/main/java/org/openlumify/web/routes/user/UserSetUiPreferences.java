package org.openlumify.web.routes.user;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Required;
import org.json.JSONObject;
import org.openlumify.core.model.user.UserRepository;
import org.openlumify.core.user.User;
import org.openlumify.web.OpenLumifyResponse;
import org.openlumify.web.clientapi.model.ClientApiSuccess;

@Singleton
public class UserSetUiPreferences implements ParameterizedHandler {
    private final UserRepository userRepository;

    @Inject
    public UserSetUiPreferences(final UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Handle
    public ClientApiSuccess handle(
            User user,
            @Required(name = "name") String propertyName,
            @Required(name = "value") String propertyValue
    ) throws Exception {
        JSONObject preferences = user.getUiPreferences();
        preferences.put(propertyName, propertyValue);
        userRepository.setUiPreferences(user, preferences);
        return OpenLumifyResponse.SUCCESS;
    }
}
