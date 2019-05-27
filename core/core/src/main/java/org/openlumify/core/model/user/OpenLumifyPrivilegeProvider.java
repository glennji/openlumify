package org.openlumify.core.model.user;

import org.openlumify.web.clientapi.model.Privilege;

public class OpenLumifyPrivilegeProvider implements PrivilegesProvider {
    @Override
    public Iterable<Privilege> getPrivileges() {
        return Privilege.ALL_BUILT_IN;
    }
}
