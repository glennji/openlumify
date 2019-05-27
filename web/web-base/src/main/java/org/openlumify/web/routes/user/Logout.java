package org.openlumify.web.routes.user;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.core.security.AuditService;
import org.openlumify.core.user.User;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;
import org.openlumify.web.CurrentUser;
import org.openlumify.web.OpenLumifyResponse;
import org.openlumify.web.auth.AuthTokenHttpResponse;
import org.openlumify.web.clientapi.model.ClientApiSuccess;
import org.openlumify.webster.ParameterizedHandler;
import org.openlumify.webster.annotations.Handle;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Singleton
public class Logout implements ParameterizedHandler {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(Logout.class);

    private final AuditService auditService;

    @Inject
    public Logout(
            AuditService auditService
    ) {
        this.auditService = auditService;
    }

    @Handle
    public ClientApiSuccess handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
        User user = CurrentUser.get(request);
        if (user != null) {
            auditService.auditLogout(user.getUserId());
            CurrentUser.set(request, null);

            if (response instanceof AuthTokenHttpResponse) {
                AuthTokenHttpResponse authResponse = (AuthTokenHttpResponse) response;
                authResponse.invalidateAuthentication();
            } else {
                LOGGER.error("Logout called but response is not an instance of %s. User may not actually be logged out.", AuthTokenHttpResponse.class.getName());
            }
        }

        return OpenLumifyResponse.SUCCESS;
    }
}
