package org.openlumify.mimeTypeOntologyMapper;

import org.vertexium.Element;
import org.vertexium.Metadata;
import org.vertexium.Property;
import org.vertexium.mutation.ExistingElementMutation;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.ingest.graphProperty.GraphPropertyWorkData;
import org.openlumify.core.ingest.graphProperty.GraphPropertyWorker;
import org.openlumify.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import org.openlumify.core.model.Description;
import org.openlumify.core.model.Name;
import org.openlumify.core.model.ontology.Concept;
import org.openlumify.core.model.properties.OpenLumifyProperties;
import org.openlumify.core.model.properties.types.OpenLumifyPropertyUpdate;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.openlumify.core.model.ontology.OntologyRepository.PUBLIC;

@Name("MIME Type Ontology Mapper")
@Description("Maps MIME types to an ontology class")
public class MimeTypeOntologyMapperGraphPropertyWorker extends GraphPropertyWorker {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(MimeTypeOntologyMapperGraphPropertyWorker.class);
    public static final String DEFAULT_MAPPING_KEY = "default";
    public static final String MAPPING_INTENT_KEY = "intent";
    public static final String MAPPING_IRI_KEY = "iri";
    public static final String MAPPING_REGEX_KEY = "regex";
    private Concept defaultConcept;
    private List<MimeTypeMatcher> mimeTypeMatchers = new ArrayList<>();

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);
        loadMappings();
        logMappings();
    }

    private void logMappings() {
        for (MimeTypeMatcher matcher : mimeTypeMatchers) {
            LOGGER.debug("Matcher: %s", matcher.toString());
        }
        if (defaultConcept == null) {
            LOGGER.debug("No default concept");
        } else {
            LOGGER.debug("Default concept: %s", defaultConcept);
        }
    }

    private void loadMappings() {
        Map<String, Map<String, String>> mappings = getConfiguration().getMultiValue(MimeTypeOntologyMapperGraphPropertyWorker.class.getName() + ".mapping");
        for (Map.Entry<String, Map<String, String>> mapping : mappings.entrySet()) {
            Concept concept = getConceptFromMapping(mapping.getValue());
            if (DEFAULT_MAPPING_KEY.equals(mapping.getKey())) {
                defaultConcept = concept;
                continue;
            }

            String regex = mapping.getValue().get(MAPPING_REGEX_KEY);
            if (regex != null) {
                mimeTypeMatchers.add(new RegexMimeTypeMatcher(concept, regex));
                continue;
            }

            throw new OpenLumifyException("Expected mapping name of " + DEFAULT_MAPPING_KEY + " or a " + MAPPING_REGEX_KEY);
        }
    }

    private Concept getConceptFromMapping(Map<String, String> mapping) {
        String intent = mapping.get(MAPPING_INTENT_KEY);
        if (intent != null) {
            return getOntologyRepository().getRequiredConceptByIntent(intent, PUBLIC);
        }

        String iri = mapping.get(MAPPING_IRI_KEY);
        if (iri != null) {
            return getOntologyRepository().getRequiredConceptByIRI(iri, PUBLIC);
        }

        throw new OpenLumifyException("Missing concept for mapping. Must specify " + MAPPING_INTENT_KEY + " or " + MAPPING_IRI_KEY + ".");
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        String mimeType = OpenLumifyProperties.MIME_TYPE.getOnlyPropertyValue(data.getElement());
        Concept concept = null;

        for (MimeTypeMatcher matcher : this.mimeTypeMatchers) {
            if (matcher.matches(mimeType)) {
                concept = matcher.getConcept();
                break;
            }
        }

        if (concept == null) {
            concept = defaultConcept;
        }

        if (concept == null) {
            LOGGER.debug("skipping, no concept mapped for vertex " + data.getElement().getId());
            return;
        }

        LOGGER.debug("assigning concept type %s to vertex %s", concept.getIRI(), data.getElement().getId());

        List<OpenLumifyPropertyUpdate> changedProperties = new ArrayList<>();
        ExistingElementMutation<Element> m = data.getElement().prepareMutation();
        OpenLumifyProperties.CONCEPT_TYPE.updateProperty(changedProperties, data.getElement(), m, concept.getIRI(), (Metadata) null, getVisibilityTranslator().getDefaultVisibility());
        m.save(getAuthorizations());
        getGraph().flush();
        getWorkQueueRepository().pushGraphOpenLumifyPropertyQueue(
                data.getElement(),
                changedProperties,
                data.getWorkspaceId(),
                data.getVisibilitySource(),
                data.getPriority()
        );
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        if (!property.getName().equals(OpenLumifyProperties.RAW.getPropertyName())) {
            return false;
        }

        if (!OpenLumifyProperties.MIME_TYPE.hasProperty(element)) {
            return false;
        }

        return !OpenLumifyProperties.CONCEPT_TYPE.hasConceptType(element);
    }

    private static abstract class MimeTypeMatcher {
        private final Concept concept;

        MimeTypeMatcher(Concept concept) {
            this.concept = concept;
        }

        Concept getConcept() {
            return concept;
        }

        public abstract boolean matches(String mimeType);
    }

    private static class RegexMimeTypeMatcher extends MimeTypeMatcher {
        private final Pattern regex;

        RegexMimeTypeMatcher(Concept concept, String regex) {
            super(concept);
            this.regex = Pattern.compile(regex);
        }

        @Override
        public boolean matches(String mimeType) {
            return regex.matcher(mimeType).matches();
        }

        @Override
        public String toString() {
            return "RegexMimeTypeMatcher{" +
                    "concept=" + getConcept() +
                    ", regex=" + regex +
                    '}';
        }
    }
}
