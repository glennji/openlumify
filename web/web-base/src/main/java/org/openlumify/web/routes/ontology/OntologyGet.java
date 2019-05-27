package org.openlumify.web.routes.ontology;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.webster.annotations.Handle;
import org.openlumify.webster.annotations.Optional;
import org.openlumify.core.model.ontology.Concept;
import org.openlumify.core.model.ontology.OntologyProperty;
import org.openlumify.core.model.ontology.OntologyRepository;
import org.openlumify.core.model.ontology.Relationship;
import org.openlumify.web.clientapi.model.ClientApiOntology;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;

import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class OntologyGet extends OntologyBase {
    @Inject
    public OntologyGet(final OntologyRepository ontologyRepository) {
        super(ontologyRepository);
    }

    @Handle
    public ClientApiOntology handle(
            @Optional(name = "propertyIds[]") String[] propertyIds,
            @Optional(name = "conceptIds[]") String[] conceptIds,
            @Optional(name = "relationshipIds[]") String[] relationshipIds,
            @ActiveWorkspaceId String workspaceId
    ) {
        ClientApiOntology clientApiOntology = new ClientApiOntology();

        List<Concept> concepts = ontologyIdsToConcepts(conceptIds, workspaceId);
        List<ClientApiOntology.Concept> clientConcepts = concepts.stream()
                .map(Concept::toClientApi)
                .collect(Collectors.toList());
        clientApiOntology.addAllConcepts(clientConcepts);

        List<Relationship> relationships = ontologyIdsToRelationships(relationshipIds, workspaceId);
        List<ClientApiOntology.Relationship> clientRelationships = relationships.stream()
                .map(Relationship::toClientApi)
                .collect(Collectors.toList());
        clientApiOntology.addAllRelationships(clientRelationships);

        List<OntologyProperty> properties = ontologyIdsToProperties(propertyIds, workspaceId);
        List<ClientApiOntology.Property> clientProperties = properties.stream()
                .map(OntologyProperty::toClientApi)
                .collect(Collectors.toList());
        clientApiOntology.addAllProperties(clientProperties);

        return clientApiOntology;
    }
}
