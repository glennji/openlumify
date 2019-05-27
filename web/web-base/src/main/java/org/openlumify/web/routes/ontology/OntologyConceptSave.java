package org.openlumify.web.routes.ontology;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.webster.ParameterizedHandler;
import org.openlumify.webster.annotations.Handle;
import org.openlumify.webster.annotations.Optional;
import org.openlumify.webster.annotations.Required;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.model.ontology.Concept;
import org.openlumify.core.model.ontology.OntologyRepository;
import org.openlumify.core.model.workQueue.WorkQueueRepository;
import org.openlumify.core.user.User;
import org.openlumify.web.clientapi.model.ClientApiOntology;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;

@Singleton
public class OntologyConceptSave implements ParameterizedHandler {
    private final OntologyRepository ontologyRepository;
    private final WorkQueueRepository workQueueRepository;

    @Inject
    public OntologyConceptSave(
            final OntologyRepository ontologyRepository,
            final WorkQueueRepository workQueueRepository
    ) {
        this.ontologyRepository = ontologyRepository;
        this.workQueueRepository = workQueueRepository;
    }

    @Handle
    public ClientApiOntology.Concept handle(
            @Required(name = "displayName", allowEmpty = false) String displayName,
            @Optional(name = "iri", allowEmpty = false) String iri,
            @Optional(name = "parentConcept", allowEmpty = false) String parentConcept,
            @Optional(name = "glyphIconHref", allowEmpty = false) String glyphIconHref,
            @Optional(name = "color", allowEmpty = false) String color,
            @ActiveWorkspaceId String workspaceId,
            User user
    ) {
        Concept parent;
        if (parentConcept == null) {
            parent = ontologyRepository.getEntityConcept(workspaceId);
            parentConcept = parent.getIRI();
        } else {
            parent = ontologyRepository.getConceptByIRI(parentConcept, workspaceId);
            if (parent == null) {
                throw new OpenLumifyException("Unable to find parent concept with IRI: " + parentConcept);
            }
        }

        if (iri == null) {
            iri = ontologyRepository.generateDynamicIri(Concept.class, displayName, workspaceId, parentConcept);
        }

        Concept concept = ontologyRepository.getOrCreateConcept(parent, iri, displayName, glyphIconHref, color, null, user, workspaceId);

        ontologyRepository.clearCache(workspaceId);
        workQueueRepository.pushOntologyConceptsChange(workspaceId, concept.getId());

        return concept.toClientApi();
    }
}
