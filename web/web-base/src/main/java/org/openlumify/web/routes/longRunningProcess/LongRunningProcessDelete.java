package org.openlumify.web.routes.longRunningProcess;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.webster.ParameterizedHandler;
import org.openlumify.webster.annotations.Handle;
import org.openlumify.webster.annotations.Required;
import org.json.JSONObject;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.model.longRunningProcess.LongRunningProcessRepository;
import org.openlumify.core.user.User;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;
import org.openlumify.web.OpenLumifyResponse;
import org.openlumify.web.clientapi.model.ClientApiSuccess;

@Singleton
public class LongRunningProcessDelete implements ParameterizedHandler {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(LongRunningProcessDelete.class);
    private final LongRunningProcessRepository longRunningProcessRepository;

    @Inject
    public LongRunningProcessDelete(final LongRunningProcessRepository longRunningProcessRepository) {
        this.longRunningProcessRepository = longRunningProcessRepository;
    }

    @Handle
    public ClientApiSuccess handle(
            User authUser,
            @Required(name = "longRunningProcessId") String longRunningProcessId
    ) throws Exception {
        LOGGER.info("deleting long running process: %s", longRunningProcessId);
        JSONObject longRunningProcess = longRunningProcessRepository.findById(longRunningProcessId, authUser);
        if (longRunningProcess == null) {
            throw new OpenLumifyResourceNotFoundException("Could not find long running process: %s", longRunningProcessId);
        } else {
            LOGGER.debug("Successfully found long running process");
            longRunningProcessRepository.delete(longRunningProcessId, authUser);
            return OpenLumifyResponse.SUCCESS;
        }
    }
}
