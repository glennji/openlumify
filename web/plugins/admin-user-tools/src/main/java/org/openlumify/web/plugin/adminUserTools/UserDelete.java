package org.openlumify.web.plugin.adminUserTools;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Required;
import org.vertexium.Graph;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.model.user.UserRepository;
import org.openlumify.core.user.User;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;
import org.openlumify.web.OpenLumifyResponse;
import org.openlumify.web.clientapi.model.ClientApiSuccess;

@Singleton
public class UserDelete implements ParameterizedHandler {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(UserDelete.class);
    private final Graph graph;
    private final UserRepository userRepository;

    @Inject
    public UserDelete(final Graph graph, final UserRepository userRepository) {
        this.graph = graph;
        this.userRepository = userRepository;
    }

    @Handle
    public ClientApiSuccess handle(
            @Required(name = "user-name") String userName
    ) throws Exception {
        User user = userRepository.findByUsername(userName);
        if (user == null) {
            throw new OpenLumifyResourceNotFoundException("Could find user: " + userName);
        }

        LOGGER.info("deleting user %s", user.getUserId());
        userRepository.delete(user);
        this.graph.flush();

        return OpenLumifyResponse.SUCCESS;
    }
}
