#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.worker;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.*;
import org.vertexium.id.QueueIdGenerator;
import org.vertexium.inmemory.InMemoryAuthorizations;
import org.vertexium.property.StreamingPropertyValue;
import org.vertexium.query.Query;
import org.vertexium.query.SortDirection;
import org.openlumify.core.model.properties.OpenLumifyProperties;
import org.openlumify.core.user.User;
import org.openlumify.core.util.OpenLumifyInMemoryGPWTestBase;
import org.openlumify.web.clientapi.model.VisibilityJson;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import static ${package}.worker.OntologyConstants.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.openlumify.core.util.StreamUtil.stream;

@RunWith(MockitoJUnitRunner.class)
public class ExampleGraphPropertyWorkerTest extends OpenLumifyInMemoryGPWTestBase {
    private static final String VISIBILITY_SOURCE = "TheVisibilitySource";
    private static final String WORKSPACE_ID = "WORKSPACE_ID";
    private Visibility visibility;
    private Authorizations authorizations;
    private Vertex archiveVertex;
    private ExampleGraphPropertyWorker worker;
    private User user;

    @Before
    public void before() throws Exception {
        QueueIdGenerator graphIdGenerator = new QueueIdGenerator();
        for (int i = 0; i < 100; i++) {
            graphIdGenerator.push("id" + i);
        }

        VisibilityJson visibilityJson = new VisibilityJson();
        visibilityJson.addWorkspace(WORKSPACE_ID);
        visibility = getVisibilityTranslator().toVisibility(visibilityJson).getVisibility();
        authorizations = new InMemoryAuthorizations(VISIBILITY_SOURCE, WORKSPACE_ID);

        archiveVertex = getGraph().addVertex(visibility, authorizations);
        OpenLumifyProperties.MIME_TYPE.addPropertyValue(archiveVertex, "", "text/csv", visibility, authorizations);
        OpenLumifyProperties.VISIBILITY_JSON.setProperty(archiveVertex, visibilityJson, visibility, authorizations);

        InputStream archiveIn = getClass().getResource("/contacts.csv").openStream();
        StreamingPropertyValue value = new StreamingPropertyValue(archiveIn, byte[].class);
        OpenLumifyProperties.RAW.setProperty(archiveVertex, value, visibility, authorizations);
        archiveIn.close();

        user = getUserRepository().findOrAddUser("test", "test", "test@test.com", "test");
        getWorkspaceRepository().add(WORKSPACE_ID, "Test", user);

        worker = new ExampleGraphPropertyWorker();
    }

    @Test
    public void isHandledReturnsTrueForRawPropertyWithCsvMimeType() throws Exception {
        boolean handled = worker.isHandled(archiveVertex, OpenLumifyProperties.RAW.getProperty(archiveVertex));

        assertThat(handled, is(true));
    }

    @Test
    public void isHandledReturnsFalseForRawPropertyWithOtherMimeType() throws Exception {
        OpenLumifyProperties.MIME_TYPE.removeProperty(archiveVertex, "", authorizations);
        OpenLumifyProperties.MIME_TYPE.addPropertyValue(
                archiveVertex, "", "application/octet-stream", visibility, authorizations);

        boolean handled = worker.isHandled(archiveVertex, OpenLumifyProperties.RAW.getProperty(archiveVertex));

        assertThat(handled, is(false));
    }

    @Test
    public void isHandledReturnsFalseForNullProperty() throws Exception {
        assertThat(worker.isHandled(archiveVertex, null), is(false));
    }

    @Test
    public void executeShouldCreatePersonVerticesFromContactsCsvFileVertex() throws Exception {
        run(worker, createWorkerPrepareData(null, user, null, null), archiveVertex, WORKSPACE_ID);

        Query csvFileQuery = getGraph().query(authorizations)
                .has(OpenLumifyProperties.CONCEPT_TYPE.getPropertyName(), CONTACTS_CSV_FILE_CONCEPT_TYPE);
        List<Vertex> csvFileVertices = stream(csvFileQuery.vertices()).collect(Collectors.toList());
        assertThat(csvFileVertices.size(), is(1));
        Vertex csvFileVertex = csvFileVertices.get(0);

        Query personQuery = getGraph().query(authorizations)
                .has(OpenLumifyProperties.CONCEPT_TYPE.getPropertyName(), PERSON_CONCEPT_TYPE)
                .sort(PERSON_FULL_NAME_PROPERTY.getPropertyName(), SortDirection.ASCENDING);
        List<Vertex> personVertices = stream(personQuery.vertices()).collect(Collectors.toList());
        assertThat(personVertices.size(), is(2));

        Vertex person1Vertex = personVertices.get(0);
        assertThat(PERSON_FULL_NAME_PROPERTY.getPropertyValue(person1Vertex), is("Bruce Wayne"));
        assertThat(PERSON_EMAIL_ADDRESS_PROPERTY.getPropertyValue(person1Vertex), is("batman@example.org"));
        assertThat(PERSON_PHONE_NUMBER_PROPERTY.getPropertyValue(person1Vertex), is("888-555-0102"));
        assertThat(person1Vertex.getVisibility().hasAuthorization(WORKSPACE_ID), is(true));
        List<EdgeInfo> person1Edges = stream(
                person1Vertex.getEdgeInfos(Direction.IN, HAS_ENTITY_EDGE_LABEL, authorizations))
                .collect(Collectors.toList());
        assertThat(person1Edges.size(), is(1));
        assertThat(person1Edges.get(0).getVertexId(), is(csvFileVertex.getId()));

        Vertex person2Vertex = personVertices.get(1);
        assertThat(PERSON_FULL_NAME_PROPERTY.getPropertyValue(person2Vertex), is("Clark Kent"));
        assertThat(PERSON_EMAIL_ADDRESS_PROPERTY.getPropertyValue(person2Vertex), is("superman@example.org"));
        assertThat(PERSON_PHONE_NUMBER_PROPERTY.getPropertyValue(person2Vertex), is("888-555-0101"));
        assertThat(person2Vertex.getVisibility().hasAuthorization(WORKSPACE_ID), is(true));
        List<EdgeInfo> person2Edges = stream(
                person2Vertex.getEdgeInfos(Direction.IN, HAS_ENTITY_EDGE_LABEL, authorizations))
                .collect(Collectors.toList());
        assertThat(person2Edges.size(), is(1));
        assertThat(person2Edges.get(0).getVertexId(), is(csvFileVertex.getId()));
    }
}
