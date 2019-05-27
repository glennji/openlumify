package org.openlumify.web.privilegeFilters;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.core.model.user.PrivilegeRepository;
import org.openlumify.web.clientapi.model.Privilege;

@Singleton
public class HistoryReadPrivilegeFilter extends PrivilegeFilter {
    @Inject
    protected HistoryReadPrivilegeFilter(PrivilegeRepository privilegeRepository) {
        super(Privilege.newSet(Privilege.READ, Privilege.HISTORY_READ), privilegeRepository);
    }
}
