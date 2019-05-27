package org.openlumify.web.privilegeFilters;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.core.model.user.PrivilegeRepository;
import org.openlumify.web.clientapi.model.Privilege;

@Singleton
public class PublishPrivilegeFilter extends PrivilegeFilter {
    @Inject
    protected PublishPrivilegeFilter(PrivilegeRepository privilegeRepository) {
        super(Privilege.newSet(Privilege.PUBLISH), privilegeRepository);
    }
}
