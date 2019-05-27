package org.openlumify.web.clientapi;

import org.openlumify.web.clientapi.codegen.ApiException;
import org.openlumify.web.clientapi.model.ClientApiOntology;
import org.openlumify.web.clientapi.codegen.OntologyApi;

public class OntologyApiExt extends OntologyApi {
    private ClientApiOntology ontology;

    public ClientApiOntology.Concept getConcept(String conceptIri) throws ApiException {
        if (ontology == null) {
            ontology = get();
        }
        for (ClientApiOntology.Concept concept : ontology.getConcepts()) {
            if (concept.getId().equals(conceptIri)) {
                return concept;
            }
        }
        return null;
    }
}
