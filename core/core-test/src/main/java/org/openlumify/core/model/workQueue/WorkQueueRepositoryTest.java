package org.openlumify.core.model.workQueue;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.*;
import org.vertexium.inmemory.InMemoryGraph;
import org.openlumify.core.config.Configuration;
import org.openlumify.core.ingest.graphProperty.GraphPropertyMessage;
import org.openlumify.core.model.WorkQueueNames;
import org.openlumify.core.model.properties.OpenLumifyProperties;
import org.openlumify.core.model.properties.types.OpenLumifyPropertyUpdate;
import org.openlumify.core.model.user.*;
import org.openlumify.core.model.workspace.Workspace;
import org.openlumify.core.model.workspace.WorkspaceRepository;
import org.openlumify.core.user.User;
import org.openlumify.web.clientapi.model.ClientApiWorkspace;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class WorkQueueRepositoryTest {
    private TestWorkQueueRepository workQueueRepository;
    private Graph graph;
    private Authorizations authorizations;

    @Mock
    private WorkQueueNames workQueueNames;

    @Mock
    private Configuration configuration;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthorizationRepository authorizationRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private User mockUser1;

    @Mock
    private User mockUser2;

    @Mock
    private Workspace workspace;

    @Before
    public void before() {
        graph = InMemoryGraph.create();
        authorizations = graph.createAuthorizations();
        workQueueRepository = new TestWorkQueueRepository(
                graph,
                workQueueNames,
                configuration
        );
        workQueueRepository.setAuthorizationRepository(authorizationRepository);
        workQueueRepository.setUserRepository(userRepository);
        workQueueRepository.setWorkspaceRepository(workspaceRepository);
    }

    @Test
    public void testPushWorkspaceChangeSameUser() {
        ClientApiWorkspace workspace = new ClientApiWorkspace();
        List<ClientApiWorkspace.User> previousUsers = new ArrayList<>();
        ClientApiWorkspace.User previousUser = new ClientApiWorkspace.User();
        previousUser.setUserId("user123");
        previousUsers.add(previousUser);
        String changedByUserId = "user123";
        String changedBySourceGuid = "123-123-1234";

        workQueueRepository.pushWorkspaceChange(workspace, previousUsers, changedByUserId, changedBySourceGuid);

        assertEquals(1, workQueueRepository.broadcastJsonValues.size());
        JSONObject json = workQueueRepository.broadcastJsonValues.get(0);
        assertEquals("workspaceChange", json.getString("type"));
        assertEquals("user123", json.getString("modifiedBy"));
        assertEquals(new JSONObject("{\"users\":[\"user123\"]}").toString(), json.getJSONObject("permissions").toString());
        assertEquals(
                new JSONObject("{\"editable\":false,\"users\":[],\"commentable\":false,\"sharedToUser\":false}").toString(),
                json.getJSONObject("data").toString()
        );
        assertEquals("123-123-1234", json.getString("sourceGuid"));
    }

    @Test
    public void testPushWorkspaceChangeDifferentUser() {
        ClientApiWorkspace clientApiWorkspace = new ClientApiWorkspace();
        clientApiWorkspace.setWorkspaceId("ws1");
        List<ClientApiWorkspace.User> previousUsers = new ArrayList<>();
        ClientApiWorkspace.User previousUser = new ClientApiWorkspace.User();
        previousUser.setUserId("mockUser1");
        previousUsers.add(previousUser);
        String changedByUserId = "mockUser2";
        String changedBySourceGuid = "123-123-1234";

        Authorizations mockUser1Auths = graph.createAuthorizations("mockUser1Auths");

        when(userRepository.findById(changedByUserId)).thenReturn(mockUser2);
        when(workspaceRepository.findById(eq("ws1"), eq(mockUser2))).thenReturn(workspace);
        when(userRepository.findById(eq("mockUser1"))).thenReturn(mockUser1);
        when(authorizationRepository.getGraphAuthorizations(eq(mockUser1), eq("ws1"))).thenReturn(mockUser1Auths);
        when(workspaceRepository.toClientApi(eq(workspace), eq(mockUser1), any())).thenReturn(clientApiWorkspace);

        workQueueRepository.pushWorkspaceChange(clientApiWorkspace, previousUsers, changedByUserId, changedBySourceGuid);

        assertEquals(1, workQueueRepository.broadcastJsonValues.size());
        JSONObject json = workQueueRepository.broadcastJsonValues.get(0);
        assertEquals("workspaceChange", json.getString("type"));
        assertEquals("mockUser2", json.getString("modifiedBy"));
        assertEquals(new JSONObject("{\"users\":[\"mockUser1\"]}").toString(), json.getJSONObject("permissions").toString());
        assertEquals(
                new JSONObject("{\"editable\":false,\"users\":[],\"commentable\":false,\"workspaceId\":\"ws1\",\"sharedToUser\":false}").toString(),
                json.getJSONObject("data").toString()
        );
        assertEquals("123-123-1234", json.getString("sourceGuid"));
    }

    @Test
    public void testPushGraphOpenLumifyPropertyQueue() {
        Visibility visibility = new Visibility("");
        VertexBuilder m = graph.prepareVertex("v1", visibility);
        OpenLumifyProperties.COMMENT.addPropertyValue(m, "k1", "comment1", visibility);
        OpenLumifyProperties.COMMENT.addPropertyValue(m, "k2", "comment2", visibility);
        OpenLumifyProperties.COMMENT.addPropertyValue(m, "k3", "comment3", visibility);
        Vertex element = m.save(authorizations);

        List<OpenLumifyPropertyUpdate> properties = new ArrayList<>();
        properties.add(new OpenLumifyPropertyUpdate(OpenLumifyProperties.COMMENT, "k1"));
        properties.add(new OpenLumifyPropertyUpdate(OpenLumifyProperties.COMMENT, "k2"));
        properties.add(new OpenLumifyPropertyUpdate(OpenLumifyProperties.COMMENT, "k3"));
        workQueueRepository.pushGraphOpenLumifyPropertyQueue(element, properties, Priority.HIGH);

        assertEquals(1, workQueueRepository.getWorkQueue(workQueueNames.getGraphPropertyQueueName()).size());
        GraphPropertyMessage message = GraphPropertyMessage.create(workQueueRepository.getWorkQueue(workQueueNames.getGraphPropertyQueueName()).get(0));
        assertEquals(3, message.getProperties().length);
    }

}
