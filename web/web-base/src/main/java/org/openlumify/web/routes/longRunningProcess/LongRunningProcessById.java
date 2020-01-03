package org.openlumify.web.routes.longRunningProcess;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Required;
import org.json.JSONObject;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.model.longRunningProcess.LongRunningProcessRepository;
import org.openlumify.core.user.User;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;

@Singleton
public class LongRunningProcessById implements ParameterizedHandler {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(LongRunningProcessById.class);
    private final LongRunningProcessRepository longRunningProcessRepository;

    @Inject
    public LongRunningProcessById(final LongRunningProcessRepository longRunningProcessRepository) {
        this.longRunningProcessRepository = longRunningProcessRepository;
    }

    @Handle
    public JSONObject handle(
            User authUser,
            @Required(name = "longRunningProcessId") String longRunningProcessId
    ) throws Exception {
        LOGGER.info("Attempting to retrieve long running process: %s", longRunningProcessId);
        JSONObject longRunningProcess = longRunningProcessRepository.findById(longRunningProcessId, authUser);
        if (longRunningProcess == null) {
            throw new OpenLumifyResourceNotFoundException("Could not find long running process: %s", longRunningProcessId);
        } else {
            LOGGER.debug("Successfully found long running process");
            return longRunningProcess;
        }
    }
}
