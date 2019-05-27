package org.openlumify.core.ingest.graphProperty;

import com.google.common.base.Charsets;
import org.vertexium.*;
import org.vertexium.property.StreamingPropertyValue;
import org.openlumify.core.model.properties.OpenLumifyProperties;
import org.openlumify.core.model.termMention.TermMentionRepository;
import org.openlumify.core.util.OpenLumifyInMemoryGPWTestBase;
import org.openlumify.web.clientapi.model.VisibilityJson;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.vertexium.util.IterableUtils.toList;

public abstract class TermMentionGraphPropertyWorkerTestBase extends OpenLumifyInMemoryGPWTestBase {
    private static final String MULTI_VALUE_KEY = TermMentionGraphPropertyWorkerTestBase.class.getName();
    protected static final String CONCEPT_IRI = "http://openlumify.org/test#regexGpwTest";

    public abstract GraphPropertyWorker getGpw() throws Exception;

    @Override
    protected Graph getGraph() {
        Graph graph = super.getGraph();
        graph.defineProperty(OpenLumifyProperties.TERM_MENTION_CONCEPT_TYPE.getPropertyName()).dataType(String.class).textIndexHint(TextIndexHint.EXACT_MATCH).define();
        graph.defineProperty(OpenLumifyProperties.TERM_MENTION_VISIBILITY_JSON.getPropertyName()).dataType(String.class).define();
        graph.defineProperty(OpenLumifyProperties.TERM_MENTION_TITLE.getPropertyName()).dataType(String.class).define();
        graph.defineProperty(OpenLumifyProperties.TERM_MENTION_PROCESS.getPropertyName()).dataType(String.class).define();
        graph.defineProperty(OpenLumifyProperties.TERM_MENTION_PROPERTY_KEY.getPropertyName()).dataType(String.class).define();
        graph.defineProperty(OpenLumifyProperties.TERM_MENTION_PROPERTY_NAME.getPropertyName()).dataType(String.class).define();
        graph.defineProperty(OpenLumifyProperties.TERM_MENTION_START_OFFSET.getPropertyName()).dataType(Integer.class).define();
        graph.defineProperty(OpenLumifyProperties.TERM_MENTION_END_OFFSET.getPropertyName()).dataType(Integer.class).define();
        return graph;
    }

    protected void doExtractionTest(String text, List<ExpectedTermMention> expectedTerms) throws Exception {
        VisibilityJson visibilityJson = new VisibilityJson("TermMentionGraphPropertyWorkerTestBase");
        Visibility visibility = getVisibilityTranslator().toVisibility(visibilityJson).getVisibility();
        Authorizations authorizations = getGraph().createAuthorizations("TermMentionGraphPropertyWorkerTestBase");
        Authorizations termMentionAuthorizations = getGraph().createAuthorizations(authorizations, TermMentionRepository.VISIBILITY_STRING);

        VertexBuilder vertexBuilder = getGraph().prepareVertex("v1", visibility);

        Metadata textMetadata = new Metadata();
        OpenLumifyProperties.MIME_TYPE_METADATA.setMetadata(textMetadata, "text/plain", getVisibilityTranslator().getDefaultVisibility());
        StreamingPropertyValue textPropertyValue = StreamingPropertyValue.create(asStream(text), String.class);
        OpenLumifyProperties.TEXT.addPropertyValue(vertexBuilder, MULTI_VALUE_KEY, textPropertyValue, textMetadata, visibility);

        OpenLumifyProperties.VISIBILITY_JSON.setProperty(vertexBuilder, visibilityJson, getVisibilityTranslator().getDefaultVisibility());

        Vertex vertex = vertexBuilder.save(authorizations);
        Property property = vertex.getProperty(OpenLumifyProperties.TEXT.getPropertyName());
        run(getGpw(), createWorkerPrepareData(), vertex, property, asStream(text));

        List<Vertex> termMentions = toList(vertex.getVertices(Direction.OUT,
                OpenLumifyProperties.TERM_MENTION_LABEL_HAS_TERM_MENTION, termMentionAuthorizations));

        if (expectedTerms != null && !expectedTerms.isEmpty()) {
            assertEquals("Incorrect number of terms extracted", expectedTerms.size(), termMentions.size());

            String conceptTypes = termMentions.stream()
                    .map(OpenLumifyProperties.TERM_MENTION_CONCEPT_TYPE::getPropertyValue)
                    .distinct()
                    .collect(Collectors.joining(", "));
            assertEquals("Incorrect concept types for term mentions", CONCEPT_IRI, conceptTypes);

            for (ExpectedTermMention expectedTerm : expectedTerms) {
                List<Vertex> matchingTermVertices = termMentions.stream()
                        .filter(termVertex -> expectedTerm.term.equals(OpenLumifyProperties.TERM_MENTION_TITLE.getPropertyValue(termVertex)))
                        .filter(termVertex -> expectedTerm.startOffset.equals(OpenLumifyProperties.TERM_MENTION_START_OFFSET.getPropertyValue(termVertex)))
                        .collect(Collectors.toList());

                if (matchingTermVertices.size() != 1) {
                    String foundTerms = termMentions.stream().map(termVertex ->
                            OpenLumifyProperties.TERM_MENTION_TITLE.getPropertyValue(termVertex) + '[' +
                                    OpenLumifyProperties.TERM_MENTION_START_OFFSET.getPropertyValue(termVertex) + ':' +
                                    OpenLumifyProperties.TERM_MENTION_END_OFFSET.getPropertyValue(termVertex) + ']'
                    ).collect(Collectors.joining(", "));

                    fail("Unable to find expected term " + expectedTerm + ". Found: " + foundTerms);
                }
            }
        } else {
            assertTrue("Terms extracted when there were none expected", termMentions.isEmpty());
        }
    }

    private InputStream asStream(final String text) {
        return new ByteArrayInputStream(text.getBytes(Charsets.UTF_8));
    }

    public class ExpectedTermMention {
        String term;
        Long startOffset;
        Long endOffset;

        public ExpectedTermMention(String term, Long startOffset, Long endOffset) {
            this.term = term;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
        }

        @Override
        public String toString() {
            return term + '[' + startOffset + ':' + endOffset + ']';
        }
    }
}
