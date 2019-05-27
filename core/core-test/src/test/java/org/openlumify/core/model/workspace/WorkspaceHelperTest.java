package org.openlumify.core.model.workspace;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.Authorizations;
import org.vertexium.Edge;
import org.vertexium.Vertex;
import org.vertexium.Visibility;
import org.vertexium.inmemory.InMemoryGraph;
import org.openlumify.core.model.ontology.OntologyRepository;
import org.openlumify.core.model.properties.OpenLumifyProperties;
import org.openlumify.core.model.termMention.TermMentionRepository;
import org.openlumify.core.model.user.*;
import org.openlumify.core.model.workQueue.Priority;
import org.openlumify.core.model.workQueue.WorkQueueRepository;
import org.openlumify.core.security.DirectVisibilityTranslator;
import org.openlumify.core.security.VisibilityTranslator;
import org.openlumify.core.user.User;
import org.openlumify.web.clientapi.model.VisibilityJson;

import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.openlumify.core.model.ontology.OntologyRepository.PUBLIC;

@RunWith(MockitoJUnitRunner.class)
public class WorkspaceHelperTest {
    private static final String WORKSPACE_ID = "WORKSPACE_1234";
    private InMemoryGraph graph;
    private Visibility visibility;
    private Visibility termMentionVisibility;
    private Authorizations authorizations;
    private VisibilityTranslator visibilityTranslator;
    private WorkspaceHelper workspaceHelper;
    private TermMentionRepository termMentionRepository;
    private GraphAuthorizationRepository authorizationsRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkQueueRepository workQueueRepository;

    @Mock
    private OntologyRepository ontologyRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private PrivilegeRepository privilegeRepository;

    @Mock
    private AuthorizationRepository authorizationRepository;

    @Mock
    private User user;

    @Before
    public void setUp() {
        graph = InMemoryGraph.create();

        visibility = new Visibility("");
        termMentionVisibility = new Visibility(TermMentionRepository.VISIBILITY_STRING);
        authorizations = graph.createAuthorizations(TermMentionRepository.VISIBILITY_STRING, WORKSPACE_ID);
        authorizationsRepository = new InMemoryGraphAuthorizationRepository();
        visibilityTranslator = new DirectVisibilityTranslator();
        termMentionRepository = new TermMentionRepository(graph, visibilityTranslator, workQueueRepository, authorizationsRepository);

        when(ontologyRepository.getRelationshipIRIByIntent("entityHasImage", PUBLIC)).thenReturn(
                "http://openlumify.org/test#entityHasImage");
        when(ontologyRepository.getRelationshipIRIByIntent("artifactContainsImageOfEntity", PUBLIC)).thenReturn(
                "http://openlumify.org/test#artifactContainsImageOfEntity");
        workspaceHelper = new WorkspaceHelper(
                termMentionRepository,
                workQueueRepository,
                graph,
                ontologyRepository,
                workspaceRepository,
                privilegeRepository,
                authorizationRepository
        );
    }

    @Test
    public void testUnresolveTerm() throws Exception {
        Vertex v1 = graph.addVertex("v1", visibility, authorizations);
        Vertex v1tm1 = graph.addVertex("v1tm1", termMentionVisibility, authorizations);
        OpenLumifyProperties.TERM_MENTION_RESOLVED_EDGE_ID.setProperty(
                v1tm1,
                "v1_to_v2",
                termMentionVisibility,
                authorizations
        );
        Vertex v2 = graph.addVertex("v2", visibility, authorizations);
        graph.addEdge(
                "v1_to_c1tm1",
                v1,
                v1tm1,
                OpenLumifyProperties.TERM_MENTION_LABEL_HAS_TERM_MENTION,
                termMentionVisibility,
                authorizations
        );
        graph.addEdge(
                "c1tm1_to_v2",
                v1tm1,
                v2,
                OpenLumifyProperties.TERM_MENTION_LABEL_RESOLVED_TO,
                termMentionVisibility,
                authorizations
        );
        Edge e = graph.addEdge("v1_to_v2", v1, v2, "link", visibility, authorizations);
        VisibilityJson visibilityJson = new VisibilityJson();
        visibilityJson.addWorkspace(WORKSPACE_ID);
        OpenLumifyProperties.VISIBILITY_JSON.setProperty(e, visibilityJson, new Visibility(""), authorizations);
        graph.flush();

        workspaceHelper.unresolveTerm(v1tm1, authorizations);
        v1tm1 = graph.getVertex("v1tm1", authorizations);
        assertNull(v1tm1);
    }

    @Test
    public void testDeletePublicVertex() throws Exception {
        Vertex doc = graph.addVertex("doc", visibility, authorizations);
        Vertex v1 = graph.addVertex("v1", visibility, authorizations);
        Vertex tm = graph.addVertex("tm", termMentionVisibility, authorizations);

        OpenLumifyProperties.TERM_MENTION_RESOLVED_EDGE_ID.setProperty(tm, "doc_to_v1", termMentionVisibility, authorizations);
        graph.addEdge("doc_to_tm", doc, tm, OpenLumifyProperties.TERM_MENTION_LABEL_HAS_TERM_MENTION, termMentionVisibility, authorizations);
        graph.addEdge("v1_to_tm", tm, v1, OpenLumifyProperties.TERM_MENTION_LABEL_RESOLVED_TO, termMentionVisibility, authorizations);
        Edge e = graph.addEdge("doc_to_v1", doc, v1, "link", visibility, authorizations);
        VisibilityJson visibilityJson = new VisibilityJson();
        OpenLumifyProperties.VISIBILITY_JSON.setProperty(e, visibilityJson, new Visibility(""), authorizations);
        graph.flush();
        workspaceHelper.deleteVertex(v1, WORKSPACE_ID, true, Priority.HIGH, authorizations, user);

        v1 = graph.getVertex("v1", authorizations);
        tm = graph.getVertex("tm", authorizations);
        assertNull(v1);
        assertNull(tm);
    }
}