package org.openlumify.web.routes.vertex;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Optional;
import org.visallo.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.model.graph.GraphRepository;
import org.openlumify.core.model.workQueue.Priority;
import org.openlumify.core.model.workQueue.WorkQueueRepository;
import org.openlumify.core.model.workspace.WorkspaceRepository;
import org.openlumify.core.security.VisibilityTranslator;
import org.openlumify.core.user.User;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;
import org.openlumify.web.OpenLumifyResponse;
import org.openlumify.web.clientapi.model.ClientApiSuccess;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;
import org.openlumify.web.util.VisibilityValidator;

import java.util.ResourceBundle;

@Singleton
public class VertexSetPropertyVisibility implements ParameterizedHandler {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(VertexSetPropertyVisibility.class);
    private final Graph graph;
    private final WorkspaceRepository workspaceRepository;
    private final VisibilityTranslator visibilityTranslator;
    private final GraphRepository graphRepository;
    private final WorkQueueRepository workQueueRepository;

    @Inject
    public VertexSetPropertyVisibility(
            Graph graph,
            WorkspaceRepository workspaceRepository,
            VisibilityTranslator visibilityTranslator,
            GraphRepository graphRepository,
            WorkQueueRepository workQueueRepository
    ) {
        this.graph = graph;
        this.workspaceRepository = workspaceRepository;
        this.visibilityTranslator = visibilityTranslator;
        this.graphRepository = graphRepository;
        this.workQueueRepository = workQueueRepository;
    }

    @Handle
    public ClientApiSuccess handle(
            @Required(name = "graphVertexId") String graphVertexId,
            @Required(name = "newVisibilitySource") String newVisibilitySource,
            @Optional(name = "oldVisibilitySource") String oldVisibilitySource,
            @Optional(name = "propertyKey") String propertyKey,
            @Required(name = "propertyName") String propertyName,
            @ActiveWorkspaceId String workspaceId,
            ResourceBundle resourceBundle,
            User user,
            Authorizations authorizations
    ) throws Exception {
        Vertex vertex = graph.getVertex(graphVertexId, authorizations);
        if (vertex == null) {
            throw new OpenLumifyResourceNotFoundException("Could not find vertex: " + graphVertexId, graphVertexId);
        }

        VisibilityValidator.validate(graph, visibilityTranslator, resourceBundle, newVisibilitySource, user, authorizations);

        // add the vertex to the workspace so that the changes show up in the diff panel
        workspaceRepository.updateEntityOnWorkspace(workspaceId, graphVertexId, user);

        Property property = graphRepository.updatePropertyVisibilitySource(
                vertex,
                propertyKey,
                propertyName,
                oldVisibilitySource,
                newVisibilitySource,
                workspaceId,
                user,
                authorizations
        );
        this.graph.flush();

        workQueueRepository.pushGraphPropertyQueue(
                vertex,
                property,
                workspaceId,
                newVisibilitySource,
                Priority.HIGH
        );

        return OpenLumifyResponse.SUCCESS;
    }
}
