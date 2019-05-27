package org.openlumify.web.routes.ontology;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlumify.web.OpenLumifyResponse;
import org.openlumify.web.clientapi.model.ClientApiOntology;
import org.openlumify.web.routes.RouteTestBase;

import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OntologyTest extends RouteTestBase {
    private static final String ETAG = "12345";

    private Ontology route;

    private ClientApiOntology expectedClientApiOntology;

    @Mock
    private OpenLumifyResponse openlumifyResponse;

    @Before
    public void before() throws IOException {
        super.before();
        route = new Ontology(ontologyRepository);

        expectedClientApiOntology = new ClientApiOntology();

        ClientApiOntology.Concept concept = new ClientApiOntology.Concept();
        concept.setId("concept-iri");
        expectedClientApiOntology.addAllConcepts(Collections.singleton(concept));

        ClientApiOntology.Relationship relationship = new ClientApiOntology.Relationship();
        relationship.setTitle("relationship-iri");
        expectedClientApiOntology.addAllRelationships(Collections.singleton(relationship));

        ClientApiOntology.Property property = new ClientApiOntology.Property();
        property.setTitle("property-iri");
        expectedClientApiOntology.addAllProperties(Collections.singleton(property));

        when(ontologyRepository.getClientApiObject(WORKSPACE_ID)).thenReturn(expectedClientApiOntology);
        when(openlumifyResponse.generateETag((byte[]) notNull())).thenReturn(ETAG);
    }

    @Test
    public void testWithNonMatchingEtagHeader() throws Exception {
        when(openlumifyResponse.testEtagHeaders(ETAG)).thenReturn(false);

        ClientApiOntology clientOntology = route.handle(WORKSPACE_ID, openlumifyResponse);

        assertSame(expectedClientApiOntology, clientOntology);
        Mockito.verify(openlumifyResponse, times(1)).addETagHeader(ETAG);
    }

    @Test
    public void testWithMatchingEtagHeader() throws Exception {
        when(openlumifyResponse.testEtagHeaders(ETAG)).thenReturn(true);

        ClientApiOntology clientOntology = route.handle(WORKSPACE_ID, openlumifyResponse);

        assertSame(expectedClientApiOntology, clientOntology);
        Mockito.verify(openlumifyResponse, never()).addETagHeader(ETAG);
    }
}
