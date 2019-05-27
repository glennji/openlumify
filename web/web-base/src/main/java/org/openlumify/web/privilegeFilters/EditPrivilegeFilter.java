package org.openlumify.web.privilegeFilters;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.core.model.user.PrivilegeRepository;
import org.openlumify.web.clientapi.model.Privilege;

@Singleton
public class EditPrivilegeFilter extends PrivilegeFilter {
    @Inject
    protected EditPrivilegeFilter(PrivilegeRepository privilegeRepository) {
        super(Privilege.newSet(Privilege.EDIT), privilegeRepository);
    }
}
