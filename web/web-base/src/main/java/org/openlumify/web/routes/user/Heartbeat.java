package org.openlumify.web.routes.user;

import com.google.inject.Singleton;
import org.openlumify.web.OpenLumifyResponse;
import org.openlumify.web.clientapi.model.ClientApiSuccess;
import org.openlumify.webster.ParameterizedHandler;
import org.openlumify.webster.annotations.Handle;

/**
 * Called by client to keep the auth token valid when there is
 * user activity, but no requests to update it.
 *
 * @see "webapp/js/data/web-worker/handlers/userActivityExtend.js"
 */
@Singleton
public class Heartbeat implements ParameterizedHandler {

    @Handle
    public ClientApiSuccess handle() throws Exception {
        return OpenLumifyResponse.SUCCESS;
    }
}
