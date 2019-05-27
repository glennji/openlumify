package org.openlumify.vertexium.model.ontology;

import org.vertexium.Authorizations;
import org.openlumify.core.config.Configuration;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.model.ontology.Concept;
import org.openlumify.core.model.ontology.OntologyRepository;
import org.openlumify.core.model.ontology.OntologyRepositoryTestBase;
import org.openlumify.core.model.user.PrivilegeRepository;
import org.openlumify.core.model.workspace.WorkspaceRepository;
import org.openlumify.core.user.SystemUser;

import static org.openlumify.core.model.user.UserRepository.USER_CONCEPT_IRI;

public class InMemoryOntologyRepositoryTest extends OntologyRepositoryTestBase {
    private InMemoryOntologyRepository ontologyRepository;

    @Override
    protected OntologyRepository getOntologyRepository() {
        if (ontologyRepository != null) {
            return ontologyRepository;
        }
        try {
            ontologyRepository = new InMemoryOntologyRepository(
                    getGraph(),
                    getConfiguration(),
                    getLockRepository(),
                    getCacheService()
            ) {
                @Override
                public void loadOntologies(Configuration config, Authorizations authorizations) throws Exception {
                    SystemUser systemUser = new SystemUser();
                    Concept rootConcept = getOrCreateConcept(null, ROOT_CONCEPT_IRI, "root", null, systemUser, PUBLIC);
                    getOrCreateConcept(rootConcept, ENTITY_CONCEPT_IRI, "thing", null, systemUser, PUBLIC);
                    getOrCreateConcept(null, USER_CONCEPT_IRI, "openlumifyUser", null, false, systemUser, PUBLIC);
                    defineRequiredProperties(getGraph());
                    clearCache();
                }

                @Override
                protected PrivilegeRepository getPrivilegeRepository() {
                    return InMemoryOntologyRepositoryTest.this.getPrivilegeRepository();
                }

                @Override
                protected WorkspaceRepository getWorkspaceRepository() {
                    return InMemoryOntologyRepositoryTest.this.getWorkspaceRepository();
                }
            };
        } catch (Exception ex) {
            throw new OpenLumifyException("Could not create ontology repository", ex);
        }
        return ontologyRepository;
    }
}
