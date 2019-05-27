package org.openlumify.core.security;

import org.openlumify.core.user.User;

public interface AuditService {
    void auditLogin(User user);

    void auditLogout(String userId);

    void auditAccessDenied(String message, User user, Object resourceId);
}
