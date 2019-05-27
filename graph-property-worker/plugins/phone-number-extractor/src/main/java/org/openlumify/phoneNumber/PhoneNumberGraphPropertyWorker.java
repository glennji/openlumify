package org.openlumify.phoneNumber;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.i18n.phonenumbers.PhoneNumberMatch;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import org.vertexium.Element;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.openlumify.core.ingest.graphProperty.GraphPropertyWorkData;
import org.openlumify.core.ingest.graphProperty.GraphPropertyWorker;
import org.openlumify.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import org.openlumify.core.model.Description;
import org.openlumify.core.model.Name;
import org.openlumify.core.model.properties.OpenLumifyProperties;
import org.openlumify.core.model.termMention.TermMentionBuilder;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;
import org.openlumify.web.clientapi.model.VisibilityJson;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static org.vertexium.util.IterableUtils.count;
import static org.openlumify.core.model.ontology.OntologyRepository.PUBLIC;

@Name("Phone Number Extractor")
@Description("Extracts phone numbers from text")
public class PhoneNumberGraphPropertyWorker extends GraphPropertyWorker {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(PhoneNumberGraphPropertyWorker.class);
    public static final String PHONE_NUMBER_CONCEPT_INTENT = "phoneNumber";
    private static final String DEFAULT_REGION_CODE = "phoneNumber.defaultRegionCode";
    private static final String DEFAULT_DEFAULT_REGION_CODE = "US";

    private final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
    private String defaultRegionCode;
    private String publicEntityType;

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);

        defaultRegionCode = (String) workerPrepareData.getConfiguration().get(DEFAULT_REGION_CODE);
        if (defaultRegionCode == null) {
            defaultRegionCode = DEFAULT_DEFAULT_REGION_CODE;
        }

        publicEntityType = getOntologyRepository().getRequiredConceptIRIByIntent(PHONE_NUMBER_CONCEPT_INTENT, PUBLIC);
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        LOGGER.debug("Extracting phone numbers from provided text");

        final String text = CharStreams.toString(new InputStreamReader(in, Charsets.UTF_8));

        Vertex outVertex = (Vertex) data.getElement();
        VisibilityJson visibilityJson = VisibilityJson.updateVisibilitySourceAndAddWorkspaceId(null, null, data.getWorkspaceId());
        final Iterable<PhoneNumberMatch> phoneNumbers = phoneNumberUtil.findNumbers(text, defaultRegionCode);
        List<Vertex> termMentions = new ArrayList<>();
        for (final PhoneNumberMatch phoneNumber : phoneNumbers) {
            final String formattedNumber = phoneNumberUtil.format(phoneNumber.number(), PhoneNumberUtil.PhoneNumberFormat.E164);
            int start = phoneNumber.start();
            int end = phoneNumber.end();

            Vertex termMention = new TermMentionBuilder()
                    .outVertex(outVertex)
                    .propertyKey(data.getProperty().getKey())
                    .propertyName(data.getProperty().getName())
                    .start(start)
                    .end(end)
                    .title(formattedNumber)
                    .conceptIri(publicEntityType)
                    .visibilityJson(visibilityJson)
                    .process(getClass().getName())
                    .save(getGraph(), getVisibilityTranslator(), getUser(), getAuthorizations());
            termMentions.add(termMention);
        }
        getGraph().flush();
        applyTermMentionFilters(outVertex, termMentions);
        pushTextUpdated(data);

        LOGGER.debug("Number of phone numbers extracted: %d", count(phoneNumbers));
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        if (property.getName().equals(OpenLumifyProperties.RAW.getPropertyName())) {
            return false;
        }

        String mimeType = OpenLumifyProperties.MIME_TYPE_METADATA.getMetadataValue(property.getMetadata(), null);
        return !(mimeType == null || !mimeType.startsWith("text"));
    }
}
