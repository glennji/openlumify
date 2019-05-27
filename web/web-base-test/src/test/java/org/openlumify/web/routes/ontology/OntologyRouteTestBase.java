package org.openlumify.web.routes.ontology;

import org.junit.Before;
import org.mockito.Mock;
import org.vertexium.Authorizations;
import org.openlumify.core.cache.NopCacheService;
import org.openlumify.core.config.Configuration;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.model.lock.NonLockingLockRepository;
import org.openlumify.core.model.ontology.Concept;
import org.openlumify.core.model.ontology.OntologyPropertyDefinition;
import org.openlumify.core.model.ontology.OntologyRepository;
import org.openlumify.core.model.ontology.Relationship;
import org.openlumify.core.model.user.InMemoryGraphAuthorizationRepository;
import org.openlumify.core.model.user.PrivilegeRepository;
import org.openlumify.core.model.workspace.WorkspaceRepository;
import org.openlumify.core.security.OpenLumifyVisibility;
import org.openlumify.core.user.SystemUser;
import org.openlumify.core.user.User;
import org.openlumify.vertexium.model.ontology.VertexiumOntologyRepository;
import org.openlumify.web.clientapi.model.PropertyType;
import org.openlumify.web.routes.RouteTestBase;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.openlumify.core.model.ontology.OntologyRepository.PUBLIC;
import static org.openlumify.core.model.user.UserRepository.USER_CONCEPT_IRI;

public abstract class OntologyRouteTestBase extends RouteTestBase {
    static final String PUBLIC_CONCEPT_IRI = "public-concept-a";
    static final String PUBLIC_CONCEPT_IRI_B = "public-concept-b";
    static final String PUBLIC_RELATIONSHIP_IRI = "public-relationship";
    static final String PUBLIC_RELATIONSHIP_IRI_B = "public-relationship-b";
    static final String PUBLIC_PROPERTY_IRI = "public-property";

    @Mock
    PrivilegeRepository privilegeRepository;

    Authorizations workspaceAuthorizations;

    @Before
    public void before() throws IOException {
        super.before();

        NonLockingLockRepository nonLockingLockRepository = new NonLockingLockRepository();
        InMemoryGraphAuthorizationRepository graphAuthorizationRepository = new InMemoryGraphAuthorizationRepository();
        try {
            ontologyRepository = new VertexiumOntologyRepository(
                    graph,
                    graphRepository,
                    visibilityTranslator,
                    configuration,
                    graphAuthorizationRepository,
                    nonLockingLockRepository,
                    new NopCacheService()
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
                    return OntologyRouteTestBase.this.privilegeRepository;
                }

                @Override
                protected WorkspaceRepository getWorkspaceRepository() {
                    return OntologyRouteTestBase.this.workspaceRepository;
                }
            };
        } catch (Exception e) {
            throw new OpenLumifyException("Unable to create in ontology repository", e);
        }

        User systemUser = new SystemUser();
        Authorizations systemAuthorizations = graph.createAuthorizations(OpenLumifyVisibility.SUPER_USER_VISIBILITY_STRING);
        Concept thingConcept = ontologyRepository.getEntityConcept(OntologyRepository.PUBLIC);

        List<Concept> things = Collections.singletonList(thingConcept);
        Relationship hasEntityRel = ontologyRepository.getOrCreateRelationshipType(null, things, things, "has-entity-iri", true, systemUser, PUBLIC);
        hasEntityRel.addIntent("entityHasImage", user, systemAuthorizations);

        ontologyRepository.getOrCreateConcept(thingConcept, PUBLIC_CONCEPT_IRI, "Public A", null, systemUser, PUBLIC);
        ontologyRepository.getOrCreateConcept(thingConcept, PUBLIC_CONCEPT_IRI_B, "Public B", null, systemUser, PUBLIC);
        ontologyRepository.getOrCreateRelationshipType(null, things, things, PUBLIC_RELATIONSHIP_IRI, true, systemUser, PUBLIC);
        ontologyRepository.getOrCreateRelationshipType(null, things, things, PUBLIC_RELATIONSHIP_IRI_B, true, systemUser, PUBLIC);

        OntologyPropertyDefinition ontologyPropertyDefinition = new OntologyPropertyDefinition(things, PUBLIC_PROPERTY_IRI, "Public Property", PropertyType.DATE);
        ontologyRepository.getOrCreateProperty(ontologyPropertyDefinition, systemUser, PUBLIC);

        ontologyRepository.clearCache();

        workspaceAuthorizations = graph.createAuthorizations(WORKSPACE_ID);
    }
}
