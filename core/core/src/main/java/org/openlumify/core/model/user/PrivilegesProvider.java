package org.openlumify.core.model.user;

import org.openlumify.web.clientapi.model.Privilege;

public interface PrivilegesProvider {
    Iterable<Privilege> getPrivileges();
}
