package org.openlumify.core.model.user.cli;

import org.openlumify.core.model.user.PrivilegeRepository;

public interface PrivilegeRepositoryWithCliSupport extends PrivilegeRepository {
    PrivilegeRepositoryCliService getCliService();
}
