package org.openlumify.core.exception;

import org.openlumify.core.bootstrap.InjectHelper;
import org.openlumify.core.security.AuditService;
import org.openlumify.core.user.User;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;

public class OpenLumifyAccessDeniedException extends OpenLumifyException {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(OpenLumifyAccessDeniedException.class);
    private static final long serialVersionUID = -7805633940987966796L;
    private static AuditService auditService;
    private final User user;
    private final Object resourceId;

    public OpenLumifyAccessDeniedException(String message, User user, Object resourceId) {
        super(message);
        this.user = user;
        this.resourceId = resourceId;
        try {
            AuditService auditService = getAuditService();
            auditService.auditAccessDenied(message, user, resourceId);
        } catch (Exception ex) {
            LOGGER.error(
                    "failed to audit access denied \"%s\" (userId: %s, resourceId: %s)",
                    message,
                    user == null ? "unknown" : user.getUserId(),
                    resourceId,
                    ex
            );
        }
    }

    private AuditService getAuditService() {
        if (auditService == null) {
            auditService = InjectHelper.getInstance(AuditService.class);
        }
        return auditService;
    }

    public User getUser() {
        return user;
    }

    public Object getResourceId() {
        return resourceId;
    }
}
