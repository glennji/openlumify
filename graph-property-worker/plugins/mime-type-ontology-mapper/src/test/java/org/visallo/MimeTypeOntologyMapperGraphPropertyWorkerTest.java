package org.visallo;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.*;
import org.vertexium.property.StreamingPropertyValue;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.user.UserPropertyPrivilegeRepository;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloInMemoryGPWTestBase;
import org.visallo.mimeTypeOntologyMapper.MimeTypeOntologyMapperGraphPropertyWorker;
import org.visallo.vertexium.model.ontology.InMemoryConcept;
import org.visallo.web.clientapi.model.Privilege;
import org.visallo.web.clientapi.model.VisibilityJson;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.visallo.core.model.ontology.OntologyRepository.PUBLIC;
import static org.visallo.mimeTypeOntologyMapper.MimeTypeOntologyMapperGraphPropertyWorker.*;

@RunWith(MockitoJUnitRunner.class)
public class MimeTypeOntologyMapperGraphPropertyWorkerTest extends VisalloInMemoryGPWTestBase {
    private static final String DEFAULT_CONCEPT_IRI = "http://visallo.org/junit#defaultConcept";
    private static final String MULTIVALUE_KEY = MimeTypeOntologyMapperGraphPropertyWorkerTest.class.getName();
    private static final String TEXT_CONCEPT_IRI = "http://visallo.org/junit#textConcept";
    private static final String DEFAULT_CONFIG_KEY = MimeTypeOntologyMapperGraphPropertyWorker.class.getName() + ".mapping." + DEFAULT_MAPPING_KEY + "." + MAPPING_IRI_KEY;

    private static final String TEXT_MIME_TYPE = "text/plain";
    private static final String PNG_MIME_TYPE = "image/png";

    private MimeTypeOntologyMapperGraphPropertyWorker gpw;

    private Map<String, String> extraConfiguration = new HashMap<>();

    @Before
    public void before() throws Exception {
        super.before();
        gpw = new MimeTypeOntologyMapperGraphPropertyWorker();
    }

    @Override
    protected Map getConfigurationMap() {
        Map configurationMap = super.getConfigurationMap();
        configurationMap.putAll(extraConfiguration);
        return configurationMap;
    }

    @Test
    public void testUnknownMimeTypeWithNoDefaultConfigured() {
        Vertex vertex = run(TEXT_MIME_TYPE);
        assertFalse("GPW should not have set a concept type", VisalloProperties.CONCEPT_TYPE.hasConceptType(vertex));
    }

    @Test
    public void testUnknownMimeTypeGetsDefault() {
        extraConfiguration.put(DEFAULT_CONFIG_KEY, DEFAULT_CONCEPT_IRI);
        setupOntology();
        Vertex vertex = run(TEXT_MIME_TYPE);
        assertEquals("GPW should have set default concept type", DEFAULT_CONCEPT_IRI, VisalloProperties.CONCEPT_TYPE.getPropertyValue(vertex));
    }

    @Test
    public void testMappingWithNoRegex() {
        String configKey = MimeTypeOntologyMapperGraphPropertyWorker.class.getName() + ".mapping.texFiles." + MAPPING_IRI_KEY;
        extraConfiguration.put(DEFAULT_CONFIG_KEY, DEFAULT_CONCEPT_IRI);
        extraConfiguration.put(configKey, TEXT_CONCEPT_IRI);
        setupOntology();
        try {
            run(TEXT_MIME_TYPE);
        } catch (VisalloException ve) {
            assertTrue(ve.getMessage().contains("Failed to prepare"));
            assertTrue(ve.getCause().getMessage().contains("Expected mapping name of default or a regex"));
        }
    }

    @Test
    public void testMappingForText() {
        extraConfiguration.put(DEFAULT_CONFIG_KEY, DEFAULT_CONCEPT_IRI);
        extraConfiguration.put(MimeTypeOntologyMapperGraphPropertyWorker.class.getName() + ".mapping.texFiles." + MAPPING_INTENT_KEY, "textFile");
        extraConfiguration.put(MimeTypeOntologyMapperGraphPropertyWorker.class.getName() + ".mapping.texFiles." + MAPPING_REGEX_KEY, "text/.+");
        setupOntology();

        Vertex vertex = run(TEXT_MIME_TYPE);
        assertEquals("GPW should have set text concept type", TEXT_CONCEPT_IRI, VisalloProperties.CONCEPT_TYPE.getPropertyValue(vertex));
    }

    @Test
    public void testMappingForTextWithNonTextVertex() {
        extraConfiguration.put(DEFAULT_CONFIG_KEY, DEFAULT_CONCEPT_IRI);
        extraConfiguration.put(MimeTypeOntologyMapperGraphPropertyWorker.class.getName() + ".mapping.texFiles." + MAPPING_INTENT_KEY, "textFile");
        extraConfiguration.put(MimeTypeOntologyMapperGraphPropertyWorker.class.getName() + ".mapping.texFiles." + MAPPING_REGEX_KEY, "text/.+");
        setupOntology();

        Vertex vertex = run(PNG_MIME_TYPE);
        assertEquals("GPW should have set default concept type", DEFAULT_CONCEPT_IRI, VisalloProperties.CONCEPT_TYPE.getPropertyValue(vertex));
    }

    private void setupOntology() {
        User user = getUserRepository().findOrAddUser(
                "junit",
                "JUnit",
                "junit@v5analytics.com",
                "password"
        );
        InMemoryConcept defaultConcept = new InMemoryConcept(DEFAULT_CONCEPT_IRI, VisalloProperties.CONCEPT_TYPE_THING, null);
        ((UserPropertyPrivilegeRepository) getPrivilegeRepository()).setPrivileges(user, Sets.newHashSet(Privilege.ONTOLOGY_PUBLISH), getUserRepository().getSystemUser());
        getOntologyRepository().getOrCreateConcept(defaultConcept, DEFAULT_CONCEPT_IRI, "defaultConcept", null, user, PUBLIC);

        InMemoryConcept textConcept = new InMemoryConcept(TEXT_CONCEPT_IRI, VisalloProperties.CONCEPT_TYPE_THING, null);
        Concept textConcept1 = getOntologyRepository().getOrCreateConcept(textConcept, TEXT_CONCEPT_IRI, "textConcept", null, user, PUBLIC);
        textConcept1.addIntent("textFile", user, getGraphAuthorizations(user, VisalloVisibility.SUPER_USER_VISIBILITY_STRING));

    }

    private Vertex run(String mimeType) {
        VisibilityJson visibilityJson = new VisibilityJson("MimeTypeOntologyMapperGraphPropertyWorkerTest");
        Visibility visibility = getVisibilityTranslator().toVisibility(visibilityJson).getVisibility();
        Authorizations authorizations = getGraph().createAuthorizations("MimeTypeOntologyMapperGraphPropertyWorkerTest");

        VertexBuilder vertexBuilder = getGraph().prepareVertex("v1", visibility);

        Metadata textMetadata = new Metadata();
        VisalloProperties.MIME_TYPE_METADATA.setMetadata(textMetadata, mimeType, getVisibilityTranslator().getDefaultVisibility());
        VisalloProperties.VISIBILITY_JSON.setProperty(vertexBuilder, visibilityJson, getVisibilityTranslator().getDefaultVisibility());
        VisalloProperties.MIME_TYPE.addPropertyValue(vertexBuilder, MULTIVALUE_KEY, mimeType, getVisibilityTranslator().getDefaultVisibility());
        StreamingPropertyValue textPropertyValue = StreamingPropertyValue.create(new ByteArrayInputStream("hello".getBytes()), String.class);
        VisalloProperties.RAW.setProperty(vertexBuilder, textPropertyValue, textMetadata, visibility);

        Vertex vertex = vertexBuilder.save(authorizations);
        Property property = vertex.getProperty(VisalloProperties.RAW.getPropertyName());
        boolean didRun = run(gpw, createWorkerPrepareData(), vertex, property, null);
        assertTrue("Graph property worker didn't run", didRun);

        return getGraph().getVertex(vertex.getId(), authorizations);
    }
}
