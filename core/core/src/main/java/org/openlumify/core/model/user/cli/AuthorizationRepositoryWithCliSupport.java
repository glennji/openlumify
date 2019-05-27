package org.openlumify.core.model.user.cli;

import org.openlumify.core.model.user.AuthorizationRepository;

public interface AuthorizationRepositoryWithCliSupport extends AuthorizationRepository {
    AuthorizationRepositoryCliService getCliService();
}
