package org.openlumify.web.routes.vertex;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.model.graph.GraphRepository;
import org.openlumify.core.model.properties.OpenLumifyProperties;
import org.openlumify.core.model.termMention.TermMentionRepository;
import org.openlumify.core.model.user.PrivilegeRepository;
import org.openlumify.core.model.workQueue.Priority;
import org.openlumify.core.model.workQueue.WorkQueueRepository;
import org.openlumify.core.model.workspace.WorkspaceRepository;
import org.openlumify.core.security.VisibilityTranslator;
import org.openlumify.core.user.User;
import org.openlumify.core.util.ClientApiConverter;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;
import org.openlumify.core.util.SandboxStatusUtil;
import org.openlumify.web.clientapi.model.ClientApiSourceInfo;
import org.openlumify.web.clientapi.model.ClientApiVertex;
import org.openlumify.web.clientapi.model.Privilege;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;
import org.openlumify.web.parameterProviders.SourceGuid;
import org.openlumify.web.util.VisibilityValidator;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Required;

import java.util.ResourceBundle;
import java.util.Set;

@Singleton
public class VertexSetVisibility implements ParameterizedHandler {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(VertexSetVisibility.class);
    private final Graph graph;
    private final WorkspaceRepository workspaceRepository;
    private final WorkQueueRepository workQueueRepository;
    private final GraphRepository graphRepository;
    private final VisibilityTranslator visibilityTranslator;
    private final PrivilegeRepository privilegeRepository;
    private final TermMentionRepository termMentionRepository;

    @Inject
    public VertexSetVisibility(
            Graph graph,
            WorkspaceRepository workspaceRepository,
            WorkQueueRepository workQueueRepository,
            GraphRepository graphRepository,
            PrivilegeRepository privilegeRepository,
            VisibilityTranslator visibilityTranslator,
            TermMentionRepository termMentionRepository
    ) {
        this.graph = graph;
        this.workspaceRepository = workspaceRepository;
        this.workQueueRepository = workQueueRepository;
        this.graphRepository = graphRepository;
        this.visibilityTranslator = visibilityTranslator;
        this.privilegeRepository = privilegeRepository;
        this.termMentionRepository = termMentionRepository;
    }

    @Handle
    public ClientApiVertex handle(
            @Required(name = "graphVertexId") String graphVertexId,
            @Required(name = "visibilitySource") String visibilitySource,
            @ActiveWorkspaceId String workspaceId,
            @SourceGuid String sourceGuid,
            ResourceBundle resourceBundle,
            User user,
            Authorizations authorizations
    ) throws Exception {
        VisibilityValidator.validate(graph, visibilityTranslator, resourceBundle, visibilitySource, user, authorizations);

        Vertex graphVertex = graph.getVertex(graphVertexId, authorizations);
        if (graphVertex == null) {
            throw new OpenLumifyResourceNotFoundException("Could not find vertex: " + graphVertexId);
        }

        // add the vertex to the workspace so that the changes show up in the diff panel
        workspaceRepository.updateEntityOnWorkspace(workspaceId, graphVertexId, user);

        LOGGER.info("changing vertex (%s) visibility source to %s", graphVertex.getId(), visibilitySource);

        Set<String> privileges = privilegeRepository.getPrivileges(user);
        if (!privileges.contains(Privilege.PUBLISH)) {
            throw new OpenLumifyException("User does not have access to modify the visibility");
        }

        graphRepository.updateElementVisibilitySource(
                graphVertex,
                SandboxStatusUtil.getSandboxStatus(graphVertex, workspaceId),
                visibilitySource,
                workspaceId,
                authorizations
        );

        this.graph.flush();

        this.workQueueRepository.pushGraphPropertyQueue(
                graphVertex,
                null,
                OpenLumifyProperties.VISIBILITY_JSON.getPropertyName(),
                workspaceId,
                visibilitySource,
                Priority.HIGH
        );

        ClientApiSourceInfo sourceInfo = termMentionRepository.getSourceInfoForVertex(graphVertex, authorizations);
        if (sourceInfo != null) {
            termMentionRepository.updateEdgeVisibility(graphVertexId, visibilitySource, workspaceId, authorizations);
            workQueueRepository.pushTextUpdated(sourceInfo.vertexId);
        }

        return (ClientApiVertex) ClientApiConverter.toClientApi(graphVertex, workspaceId, authorizations);
    }
}
