package org.openlumify.core.ingest;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.*;
import org.vertexium.inmemory.InMemoryGraph;
import org.openlumify.core.config.Configuration;
import org.openlumify.core.model.WorkQueueNames;
import org.openlumify.core.model.ontology.OntologyProperty;
import org.openlumify.core.model.ontology.OntologyRepository;
import org.openlumify.core.model.properties.OpenLumifyProperties;
import org.openlumify.core.model.properties.types.IntegerOpenLumifyProperty;
import org.openlumify.core.model.workQueue.Priority;
import org.openlumify.core.model.workQueue.WorkQueueRepository;
import org.openlumify.core.model.workspace.Workspace;
import org.openlumify.core.model.workspace.WorkspaceRepository;
import org.openlumify.core.security.DirectVisibilityTranslator;
import org.openlumify.core.security.VisibilityTranslator;
import org.openlumify.core.user.User;
import org.openlumify.web.clientapi.model.ClientApiImportProperty;

import java.io.File;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.when;
import static org.vertexium.util.IterableUtils.toList;
import static org.openlumify.core.model.ontology.OntologyRepository.PUBLIC;

@RunWith(MockitoJUnitRunner.class)
public class FileImportTest {
    public static final String PROP1_NAME = "http://openlumify.org#prop1";
    private FileImport fileImport;

    private Graph graph;

    private VisibilityTranslator visibilityTranslator;

    @Mock
    private WorkQueueRepository workQueueRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkQueueNames workQueueNames;

    @Mock
    private OntologyRepository ontologyRepository;

    @Mock
    private Configuration configuration;

    @Mock
    User user;

    Authorizations authorizations;

    @Mock
    Workspace workspace;

    @Mock
    OntologyProperty ontologyProperty;

    @Before
    public void setup() {
        graph = InMemoryGraph.create();
        graph.defineProperty(OpenLumifyProperties.CONTENT_HASH.getPropertyName())
                .dataType(String.class)
                .textIndexHint(EnumSet.of(TextIndexHint.EXACT_MATCH)).define();

        visibilityTranslator = new DirectVisibilityTranslator();

        String workspaceId = "junit-workspace";
        authorizations = graph.createAuthorizations(workspaceId);

        when(workspace.getWorkspaceId()).thenReturn(workspaceId);

        when(ontologyRepository.getRequiredPropertyByIntent(PROP1_NAME, workspaceId)).thenReturn(ontologyProperty);
        when(ontologyProperty.getOpenLumifyProperty()).thenReturn(new IntegerOpenLumifyProperty(PROP1_NAME));

        fileImport = new FileImport(
                visibilityTranslator,
                graph,
                workQueueRepository,
                workspaceRepository,
                workQueueNames,
                ontologyRepository,
                configuration
        ) {
            @Override
            protected List<PostFileImportHandler> getPostFileImportHandlers() {
                return new ArrayList<>();
            }

            @Override
            protected List<FileImportSupportingFileHandler> getFileImportSupportingFileHandlers() {
                return new ArrayList<>();
            }
        };
    }

    @Test
    public void testImportVertices() throws Exception {
        File testFile = File.createTempFile("test", "test");
        try {
            FileUtils.writeStringToFile(testFile, "<html><head><title>Test HTML</title><body>Hello Test</body></html>");

            List<FileImport.FileOptions> files = new ArrayList<>();
            FileImport.FileOptions file = new FileImport.FileOptions();
            file.setConceptId("http://openlumify.org/testConcept");
            file.setFile(testFile);
            ClientApiImportProperty[] properties = new ClientApiImportProperty[1];
            properties[0] = new ClientApiImportProperty();
            properties[0].setKey("k1");
            properties[0].setName(PROP1_NAME);
            properties[0].setVisibilitySource("");
            properties[0].setValue("42");
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("m1", "v1");
            properties[0].setMetadata(metadata);
            file.setProperties(properties);
            file.setVisibilitySource("");
            files.add(file);
            Priority priority = Priority.NORMAL;
            List<Vertex> results = fileImport.importVertices(
                    workspace,
                    files,
                    priority,
                    false,
                    true,
                    user,
                    authorizations
            );
            assertEquals(1, results.size());

            Vertex v1 = graph.getVertex(results.get(0).getId(), authorizations);
            List<Property> foundProperties = toList(v1.getProperties());
            assertEquals(6, foundProperties.size());
            for (int i = 0; i < 6; i++) {
                Property foundProperty = foundProperties.get(i);
                if (foundProperty.getName().equals(PROP1_NAME)) {
                    assertEquals("k1", foundProperty.getKey());
                    assertEquals(42, foundProperty.getValue());
                    assertEquals(1, foundProperty.getMetadata().entrySet().size());
                    assertEquals("v1", foundProperty.getMetadata().getValue("m1"));
                }
            }
        } finally {
            testFile.delete();
        }
    }

    @Test
    public void testImportDuplicateFiles() throws Exception {
        boolean findExistingByFileHash = true;
        ImportTwiceResults results = importFileTwice(findExistingByFileHash);
        assertEquals(results.firstVertexId, results.secondVertexId);
    }

    @Test
    public void testImportDuplicateFilesIgnoreHash() throws Exception {
        boolean findExistingByFileHash = false;
        ImportTwiceResults results = importFileTwice(findExistingByFileHash);
        assertNotEquals(results.firstVertexId, results.secondVertexId);
    }

    private ImportTwiceResults importFileTwice(boolean findExistingByFileHash) throws Exception {
        File testFile = File.createTempFile("test", "test");
        try {
            FileUtils.writeStringToFile(testFile, "Hello World");

            List<FileImport.FileOptions> files = new ArrayList<>();
            FileImport.FileOptions file = new FileImport.FileOptions();
            file.setConceptId("http://openlumify.org/testConcept");
            file.setFile(testFile);
            file.setVisibilitySource("");
            files.add(file);

            Priority priority = Priority.NORMAL;
            List<Vertex> results = fileImport.importVertices(
                    workspace,
                    files,
                    priority,
                    false,
                    findExistingByFileHash,
                    user,
                    authorizations
            );
            assertEquals(1, results.size());
            String firstVertexId = results.get(0).getId();

            results = fileImport.importVertices(
                    workspace,
                    files,
                    priority,
                    false,
                    findExistingByFileHash,
                    user,
                    authorizations
            );
            assertEquals(1, results.size());
            String secondVertexId = results.get(0).getId();

            return new ImportTwiceResults(firstVertexId, secondVertexId);
        } finally {
            testFile.delete();
        }
    }

    private static class ImportTwiceResults {
        public final String firstVertexId;
        public final String secondVertexId;

        public ImportTwiceResults(String firstVertexId, String secondVertexId) {
            this.firstVertexId = firstVertexId;
            this.secondVertexId = secondVertexId;
        }
    }
}