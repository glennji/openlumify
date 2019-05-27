package org.openlumify.web.privilegeFilters;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.core.model.user.PrivilegeRepository;
import org.openlumify.web.clientapi.model.Privilege;

@Singleton
public class CommentPrivilegeFilter extends PrivilegeFilter {
    @Inject
    protected CommentPrivilegeFilter(PrivilegeRepository privilegeRepository) {
        super(Privilege.newSet(Privilege.COMMENT), privilegeRepository);
    }
}
