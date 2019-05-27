package org.openlumify.web.routes;

import org.json.JSONArray;
import org.mockito.Mock;
import org.vertexium.Graph;
import org.vertexium.inmemory.InMemoryGraph;
import org.openlumify.core.config.Configuration;
import org.openlumify.core.config.ConfigurationLoader;
import org.openlumify.core.config.HashMapConfigurationLoader;
import org.openlumify.core.config.OpenLumifyResourceBundleManager;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.model.graph.GraphRepository;
import org.openlumify.core.model.ontology.OntologyRepository;
import org.openlumify.core.model.termMention.TermMentionRepository;
import org.openlumify.core.model.user.UserRepository;
import org.openlumify.core.model.workQueue.WorkQueueRepository;
import org.openlumify.core.model.workspace.WorkspaceHelper;
import org.openlumify.core.model.workspace.WorkspaceRepository;
import org.openlumify.core.model.workspace.WorkspaceUser;
import org.openlumify.core.security.DirectVisibilityTranslator;
import org.openlumify.core.security.VisibilityTranslator;
import org.openlumify.core.user.User;
import org.openlumify.vertexium.model.user.InMemoryUser;
import org.openlumify.web.CurrentUser;
import org.openlumify.web.clientapi.model.WorkspaceAccess;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.openlumify.web.parameterProviders.OpenLumifyBaseParameterProvider.WORKSPACE_ID_ATTRIBUTE_NAME;

public abstract class RouteTestBase {
    public static final String WORKSPACE_ID = "WORKSPACE_12345";
    public static final String USER_ID = "USER_123";

    @Mock
    protected UserRepository userRepository;

    @Mock
    protected HttpServletRequest request;

    @Mock
    protected HttpServletResponse response;

    @Mock
    protected OntologyRepository ontologyRepository;

    @Mock
    protected WorkspaceRepository workspaceRepository;

    @Mock
    protected TermMentionRepository termMentionRepository;

    @Mock
    protected WorkspaceHelper workspaceHelper;

    @Mock
    protected WorkQueueRepository workQueueRepository;

    protected GraphRepository graphRepository;

    protected ResourceBundle resourceBundle;

    protected VisibilityTranslator visibilityTranslator;

    protected Configuration configuration;

    protected Graph graph;

    protected User user;

    protected User nonProxiedUser;

    private ByteArrayOutputStream responseByteArrayOutputStream;

    private HashMap<String, String[]> requestParameters;

    private HashMap<String, Object> attributes;

    protected void before() throws IOException {
        requestParameters = new HashMap<>();
        attributes = new HashMap<>();
        Map config = new HashMap();
        ConfigurationLoader hashMapConfigurationLoader = new HashMapConfigurationLoader(config);
        configuration = new Configuration(hashMapConfigurationLoader, new HashMap<>());

        graph = createGraph();
        visibilityTranslator = createVisibilityTranslator();
        resourceBundle = createResourceBundle();

        graphRepository = new GraphRepository(graph, visibilityTranslator, termMentionRepository, workQueueRepository);

        String currentWorkspaceId = null;
        nonProxiedUser = new InMemoryUser("jdoe", "Jane Doe", "jane.doe@email.com", currentWorkspaceId);
        when(userRepository.findById(eq(USER_ID))).thenReturn(nonProxiedUser);

        user = new InMemoryUser(USER_ID);
        when(request.getAttribute(CurrentUser.CURRENT_USER_REQ_ATTR_NAME)).thenReturn(user);

        when(request.getSession()).thenThrow(UnsupportedOperationException.class);

        when(request.getAttribute(eq(WORKSPACE_ID_ATTRIBUTE_NAME))).thenReturn(WORKSPACE_ID);

        when(workspaceRepository.hasReadPermissions(eq(WORKSPACE_ID), eq(user))).thenReturn(true);

        WorkspaceUser workspaceUser = new WorkspaceUser(user.getUserId(), WorkspaceAccess.WRITE, true);
        when(workspaceRepository.findUsersWithAccess(WORKSPACE_ID, user)).thenReturn(Collections.singletonList(workspaceUser));

        responseByteArrayOutputStream = new ByteArrayOutputStream();
        when(response.getWriter()).thenReturn(new PrintWriter(responseByteArrayOutputStream));

        when(request.getParameterNames()).thenAnswer(
                invocationOnMock -> Collections.enumeration(requestParameters.keySet())
        );
        when(request.getParameterValues(any(String.class))).thenAnswer(
                invocationOnMock -> {
                    Object key = invocationOnMock.getArguments()[0];
                    return (String[]) requestParameters.get(key);
                }
        );
        when(request.getParameter(any(String.class))).thenAnswer(
                invocationOnMock -> {
                    Object key = invocationOnMock.getArguments()[0];
                    String[] value = requestParameters.get(key);
                    if (value == null) {
                        return null;
                    }
                    if (value.length != 1) {
                        throw new OpenLumifyException("Unexpected number of values. Expected 1 found " + value.length);
                    }
                    return value[0];
                }
        );
        when(request.getParameterMap()).thenReturn(requestParameters);

        when(request.getAttributeNames()).thenAnswer(
                invocationOnMock -> Collections.enumeration(attributes.keySet())
        );
    }

    protected ResourceBundle createResourceBundle() {
        return new OpenLumifyResourceBundleManager(configuration).getBundle();
    }

    protected InMemoryGraph createGraph() {
        return InMemoryGraph.create(getGraphConfiguration());
    }

    protected DirectVisibilityTranslator createVisibilityTranslator() {
        return new DirectVisibilityTranslator();
    }

    protected Map<String, Object> getGraphConfiguration() {
        return new HashMap<>();
    }

    protected byte[] getResponse() {
        return responseByteArrayOutputStream.toByteArray();
    }

    protected void setArrayParameter(String parameterName, String[] values) {
        requestParameters.put(parameterName, values);
    }

    protected void setParameter(String parameterName, JSONArray json) {
        setParameter(parameterName, json.toString());
    }

    protected void setParameter(String parameterName, String value) {
        requestParameters.put(parameterName, new String[]{value});
    }
}
