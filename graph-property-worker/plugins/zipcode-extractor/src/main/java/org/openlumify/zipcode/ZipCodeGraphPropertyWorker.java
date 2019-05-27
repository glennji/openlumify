package org.openlumify.zipcode;

import org.openlumify.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import org.openlumify.core.ingest.graphProperty.RegexGraphPropertyWorker;
import org.openlumify.core.model.Description;
import org.openlumify.core.model.Name;
import org.openlumify.core.model.ontology.Concept;

import static org.openlumify.core.model.ontology.OntologyRepository.PUBLIC;

@Name("ZipCode Extractor")
@Description("Extracts ZipCode from text")
public class ZipCodeGraphPropertyWorker extends RegexGraphPropertyWorker {
    private static final String ZIPCODE_REG_EX = "\\b\\d{5}-\\d{4}\\b|\\b\\d{5}\\b";
    public static final String ZIPCODE_CONCEPT_INTENT = "zipCode";
    private Concept concept;

    public ZipCodeGraphPropertyWorker() {
        super(ZIPCODE_REG_EX);
    }

    @Override
    protected Concept getConcept() {
        return concept;
    }

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        this.concept = getOntologyRepository().getRequiredConceptByIntent(ZIPCODE_CONCEPT_INTENT, PUBLIC);
        super.prepare(workerPrepareData);
    }
}
