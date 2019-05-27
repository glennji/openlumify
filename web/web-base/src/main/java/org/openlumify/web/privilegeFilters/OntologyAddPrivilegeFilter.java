package org.openlumify.web.privilegeFilters;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.core.model.user.PrivilegeRepository;
import org.openlumify.web.clientapi.model.Privilege;

@Singleton
public class OntologyAddPrivilegeFilter extends PrivilegeFilter {
    @Inject
    protected OntologyAddPrivilegeFilter(PrivilegeRepository privilegeRepository) {
        super(Privilege.newSet(Privilege.ONTOLOGY_ADD), privilegeRepository);
    }
}
