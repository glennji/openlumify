package org.openlumify.core.model.ontology;

import org.json.JSONArray;
import org.json.JSONException;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.vertexium.Authorizations;
import org.vertexium.query.Query;
import org.openlumify.core.model.properties.types.OpenLumifyProperty;
import org.openlumify.core.security.OpenLumifyVisibility;
import org.openlumify.core.user.User;
import org.openlumify.web.clientapi.model.ClientApiObject;
import org.openlumify.web.clientapi.model.ClientApiOntology;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface OntologyRepository {
    String ENTITY_CONCEPT_IRI = "http://www.w3.org/2002/07/owl#Thing";
    String ROOT_CONCEPT_IRI = "http://openlumify.org#root";
    String TYPE_RELATIONSHIP = "relationship";
    String TYPE_CONCEPT = "concept";
    String TYPE_PROPERTY = "property";
    String VISIBILITY_STRING = "ontology";
    String CONFIG_INTENT_CONCEPT_PREFIX = "ontology.intent.concept.";
    String CONFIG_INTENT_RELATIONSHIP_PREFIX = "ontology.intent.relationship.";
    String CONFIG_INTENT_PROPERTY_PREFIX = "ontology.intent.property.";
    String PUBLIC = "public-ontology";
    OpenLumifyVisibility VISIBILITY = new OpenLumifyVisibility(VISIBILITY_STRING);

    void clearCache();

    void clearCache(String workspaceId);

    Iterable<Relationship> getRelationships(String workspaceId);

    Iterable<Relationship> getRelationships(Iterable<String> ids, String workspaceId);

    Iterable<OntologyProperty> getProperties(String workspaceId);

    Iterable<OntologyProperty> getProperties(Iterable<String> ids, String workspaceId);

    String getDisplayNameForLabel(String relationshipIRI, String workspaceId);

    OntologyProperty getPropertyByIRI(String propertyIRI, String workspaceId);

    Iterable<OntologyProperty> getPropertiesByIRI(List<String> propertyIRIs, String workspaceId);

    OntologyProperty getRequiredPropertyByIRI(String propertyIRI, String workspaceId);

    Relationship getRelationshipByIRI(String relationshipIRI, String workspaceId);

    Iterable<Relationship> getRelationshipsByIRI(List<String> relationshipIRIs, String workspaceId);

    boolean hasRelationshipByIRI(String relationshipIRI, String workspaceId);

    Iterable<Concept> getConceptsWithProperties(String workspaceId);

    Concept getRootConcept(String workspaceId);

    Concept getEntityConcept();

    Concept getEntityConcept(String workspaceId);

    Concept getParentConcept(Concept concept, String workspaceId);

    Set<Concept> getAncestorConcepts(Concept concept, String workspaceId);

    Set<Concept> getConceptAndAncestors(Concept concept, String workspaceId);

    Concept getConceptByIRI(String conceptIRI, String workspaceId);

    Iterable<Concept> getConceptsByIRI(List<String> conceptIRIs, String workspaceId);

    Set<Concept> getConceptAndAllChildrenByIri(String conceptIRI, String workspaceId);

    Set<Concept> getConceptAndAllChildren(Concept concept, String workspaceId);

    Set<Relationship> getRelationshipAndAllChildren(Relationship relationship, String workspaceId);

    Set<Relationship> getRelationshipAndAllChildrenByIRI(String relationshipIRI, String workspaceId);

    Relationship getParentRelationship(Relationship relationship, String workspaceId);

    Set<Relationship> getAncestorRelationships(Relationship relationship, String workspaceId);

    Set<Relationship> getRelationshipAndAncestors(Relationship relationship, String workspaceId);

    Iterable<Concept> getConcepts(Iterable<String> ids, String workspaceId);

    void deleteConcept(String conceptTypeIri, User user, String workspaceId);

    void deleteProperty(String conceptTypeIri, User user, String workspaceId);

    void deleteRelationship(String conceptTypeIri, User user, String workspaceId);

    Concept getOrCreateConcept(Concept parent, String conceptIRI, String displayName, File inDir, User user, String workspaceId);

    Concept getOrCreateConcept(Concept parent, String conceptIRI, String displayName, String glyphIconHref, String color, File inDir, User user, String workspaceId);

    Concept getOrCreateConcept(Concept parent, String conceptIRI, String displayName, File inDir, boolean deleteChangeableProperties, User user, String workspaceId);

    Concept getOrCreateConcept(Concept parent, String conceptIRI, String displayName, String glyphIconHref, String color, File inDir, boolean deleteChangeableProperties, User user, String workspaceId);

    Relationship getOrCreateRelationshipType(
            Relationship parent,
            Iterable<Concept> domainConcepts,
            Iterable<Concept> rangeConcepts,
            String relationshipIRI,
            boolean deleteChangeableProperties,
            User user,
            String workspaceId
    );

    Relationship getOrCreateRelationshipType(
            Relationship parent,
            Iterable<Concept> domainConcepts,
            Iterable<Concept> rangeConcepts,
            String relationshipIRI,
            String displayName,
            boolean deleteChangeableProperties,
            User user,
            String workspaceId
    );

    void addDomainConceptsToRelationshipType(String relationshipIri, List<String> conceptIris, User user, String workspaceId);

    void addRangeConceptsToRelationshipType(String relationshipIri, List<String> conceptIris, User user, String workspaceId);

    OntologyProperty getOrCreateProperty(OntologyPropertyDefinition ontologyPropertyDefinition, User user, String workspaceId);

    OWLOntologyManager createOwlOntologyManager(OWLOntologyLoaderConfiguration config, IRI excludeDocumentIRI) throws Exception;

    void resolvePropertyIds(JSONArray filterJson, String workspaceId) throws JSONException;

    void importResourceOwl(Class baseClass, String fileName, String iri, Authorizations authorizations);

    void importFile(File inFile, IRI documentIRI, Authorizations authorizations) throws Exception;

    void importFileData(byte[] inFileData, IRI documentIRI, File inDir, Authorizations authorizations) throws Exception;

    void writePackage(File file, IRI documentIRI, Authorizations authorizations) throws Exception;

    ClientApiOntology getClientApiObject(String workspaceId);

    Ontology getOntology(String workspaceId);

    String guessDocumentIRIFromPackage(File inFile) throws Exception;

    Concept getConceptByIntent(String intent, String workspaceId);

    String getConceptIRIByIntent(String intent, String workspaceId);

    Concept getRequiredConceptByIntent(String intent, String workspaceId);

    Concept getRequiredConceptByIRI(String iri, String workspaceId);

    String getRequiredConceptIRIByIntent(String intent, String workspaceId);

    Relationship getRelationshipByIntent(String intent, String workspaceId);

    String getRelationshipIRIByIntent(String intent, String workspaceId);

    Relationship getRequiredRelationshipByIntent(String intent, String workspaceId);

    String getRequiredRelationshipIRIByIntent(String intent, String workspaceId);

    OntologyProperty getPropertyByIntent(String intent, String workspaceId);

    String getPropertyIRIByIntent(String intent, String workspaceId);

    <T extends OpenLumifyProperty> T getOpenLumifyPropertyByIntent(String intent, Class<T> openlumifyPropertyType, String workspaceId);

    <T extends OpenLumifyProperty> T getRequiredOpenLumifyPropertyByIntent(String intent, Class<T> openlumifyPropertyType, String workspaceId);

    List<OntologyProperty> getPropertiesByIntent(String intent, String workspaceId);

    OntologyProperty getRequiredPropertyByIntent(String intent, String workspaceId);

    String getRequiredPropertyIRIByIntent(String intent, String workspaceId);

    OntologyProperty getDependentPropertyParent(String iri, String workspaceId);

    void addConceptTypeFilterToQuery(Query query, String conceptTypeIri, boolean includeChildNodes, String workspaceId);

    void addConceptTypeFilterToQuery(Query query, Collection<ElementTypeFilter> filters, String workspaceId);

    void addEdgeLabelFilterToQuery(Query query, String edgeLabel, boolean includeChildNodes, String workspaceId);

    void addEdgeLabelFilterToQuery(Query query, Collection<ElementTypeFilter> filters, String workspaceId);

    void updatePropertyDependentIris(OntologyProperty property, Collection<String> dependentPropertyIris, User user, String workspaceId);

    void updatePropertyDomainIris(OntologyProperty property, Set<String> domainIris, User user, String workspaceId);

    String generateDynamicIri(Class type, String displayName, String workspaceId, String... extended);

    void publishConcept(Concept concept, User user, String workspaceId);

    void publishRelationship(Relationship relationship, User user, String workspaceId);

    void publishProperty(OntologyProperty property, User user, String workspaceId);

    class ElementTypeFilter implements ClientApiObject {
        public String iri;
        public boolean includeChildNodes;

        public ElementTypeFilter() {

        }

        public ElementTypeFilter(String iri, boolean includeChildNodes) {
            this.iri = iri;
            this.includeChildNodes = includeChildNodes;
        }
    }
}
