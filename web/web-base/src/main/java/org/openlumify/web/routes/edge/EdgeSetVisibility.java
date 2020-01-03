package org.openlumify.web.routes.edge;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Direction;
import org.vertexium.Edge;
import org.vertexium.Graph;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.model.graph.GraphRepository;
import org.openlumify.core.model.properties.OpenLumifyProperties;
import org.openlumify.core.model.user.PrivilegeRepository;
import org.openlumify.core.model.workQueue.Priority;
import org.openlumify.core.model.workQueue.WorkQueueRepository;
import org.openlumify.core.model.workspace.WorkspaceRepository;
import org.openlumify.core.security.VisibilityTranslator;
import org.openlumify.core.user.User;
import org.openlumify.core.util.ClientApiConverter;
import org.openlumify.core.util.SandboxStatusUtil;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;
import org.openlumify.web.clientapi.model.ClientApiEdge;
import org.openlumify.web.clientapi.model.Privilege;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;
import org.openlumify.web.util.VisibilityValidator;

import java.util.ResourceBundle;
import java.util.Set;

@Singleton
public class EdgeSetVisibility implements ParameterizedHandler {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(EdgeSetVisibility.class);
    private final Graph graph;
    private final WorkQueueRepository workQueueRepository;
    private final WorkspaceRepository workspaceRepository;
    private final GraphRepository graphRepository;
    private final VisibilityTranslator visibilityTranslator;
    private final PrivilegeRepository privilegeRepository;

    @Inject
    public EdgeSetVisibility(
            Graph graph,
            WorkQueueRepository workQueueRepository,
            WorkspaceRepository workspaceRepository,
            GraphRepository graphRepository,
            VisibilityTranslator visibilityTranslator,
            PrivilegeRepository privilegeRepository
    ) {
        this.graph = graph;
        this.workQueueRepository = workQueueRepository;
        this.workspaceRepository = workspaceRepository;
        this.graphRepository = graphRepository;
        this.visibilityTranslator = visibilityTranslator;
        this.privilegeRepository = privilegeRepository;
    }

    @Handle
    public ClientApiEdge handle(
            @Required(name = "graphEdgeId") String graphEdgeId,
            @Required(name = "visibilitySource") String visibilitySource,
            @ActiveWorkspaceId String workspaceId,
            ResourceBundle resourceBundle,
            User user,
            Authorizations authorizations
    ) throws Exception {
        Edge graphEdge = graph.getEdge(graphEdgeId, authorizations);
        if (graphEdge == null) {
            throw new OpenLumifyResourceNotFoundException("Could not find edge: " + graphEdgeId);
        }

        VisibilityValidator.validate(graph, visibilityTranslator, resourceBundle, visibilitySource, user, authorizations);

        // add the vertex to the workspace so that the changes show up in the diff panel
        workspaceRepository.updateEntityOnWorkspace(workspaceId, graphEdge.getVertexId(Direction.IN), user);
        workspaceRepository.updateEntityOnWorkspace(workspaceId, graphEdge.getVertexId(Direction.OUT), user);

        LOGGER.info("changing edge (%s) visibility source to %s", graphEdge.getId(), visibilitySource);

        Set<String> privileges = privilegeRepository.getPrivileges(user);
        if (!privileges.contains(Privilege.PUBLISH)) {
            throw new OpenLumifyException("User does not have access to modify the visibility");
        }

        graphRepository.updateElementVisibilitySource(
                graphEdge,
                SandboxStatusUtil.getSandboxStatus(graphEdge, workspaceId),
                visibilitySource,
                workspaceId,
                authorizations
        );
        this.graph.flush();

        this.workQueueRepository.pushGraphPropertyQueue(
                graphEdge,
                null,
                OpenLumifyProperties.VISIBILITY_JSON.getPropertyName(),
                workspaceId,
                visibilitySource,
                Priority.HIGH
        );

        return (ClientApiEdge) ClientApiConverter.toClientApi(graphEdge, workspaceId, authorizations);
    }
}
