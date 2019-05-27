package org.openlumify.web.routes.edge;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.webster.ParameterizedHandler;
import org.openlumify.webster.annotations.Handle;
import org.openlumify.webster.annotations.Optional;
import org.openlumify.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Edge;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.openlumify.core.model.graph.GraphRepository;
import org.openlumify.core.model.workQueue.Priority;
import org.openlumify.core.model.workQueue.WorkQueueRepository;
import org.openlumify.core.security.VisibilityTranslator;
import org.openlumify.core.user.User;
import org.openlumify.core.util.ClientApiConverter;
import org.openlumify.core.util.JsonSerializer;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;
import org.openlumify.web.clientapi.model.ClientApiEdge;
import org.openlumify.web.clientapi.model.ClientApiElement;
import org.openlumify.web.clientapi.model.ClientApiSourceInfo;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;
import org.openlumify.web.parameterProviders.JustificationText;
import org.openlumify.web.util.VisibilityValidator;

import java.util.ResourceBundle;

@Singleton
public class EdgeCreate implements ParameterizedHandler {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(EdgeCreate.class);

    private final Graph graph;
    private final WorkQueueRepository workQueueRepository;
    private final GraphRepository graphRepository;
    private final VisibilityTranslator visibilityTranslator;

    @Inject
    public EdgeCreate(
            Graph graph,
            WorkQueueRepository workQueueRepository,
            GraphRepository graphRepository,
            VisibilityTranslator visibilityTranslator
    ) {
        this.graph = graph;
        this.workQueueRepository = workQueueRepository;
        this.graphRepository = graphRepository;
        this.visibilityTranslator = visibilityTranslator;
    }

    @Handle
    public ClientApiEdge handle(
            @Optional(name = "edgeId") String edgeId,
            @Required(name = "outVertexId") String outVertexId,
            @Required(name = "inVertexId") String inVertexId,
            @Required(name = "predicateLabel") String predicateLabel,
            @Required(name = "visibilitySource") String visibilitySource,
            @JustificationText String justificationText,
            ClientApiSourceInfo sourceInfo,
            @ActiveWorkspaceId String workspaceId,
            ResourceBundle resourceBundle,
            User user,
            Authorizations authorizations
    ) throws Exception {
        Vertex inVertex = graph.getVertex(inVertexId, authorizations);
        Vertex outVertex = graph.getVertex(outVertexId, authorizations);

        VisibilityValidator.validate(graph, visibilityTranslator, resourceBundle, visibilitySource, user, authorizations);

        Edge edge = graphRepository.addEdge(
                edgeId,
                outVertex,
                inVertex,
                predicateLabel,
                justificationText,
                sourceInfo,
                visibilitySource,
                workspaceId,
                user,
                authorizations
        );

        graph.flush();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Statement created:\n" + JsonSerializer.toJson(edge, workspaceId, authorizations).toString(2));
        }

        workQueueRepository.broadcastElement(edge, workspaceId);
        workQueueRepository.pushElement(edge, Priority.HIGH);
        return (ClientApiEdge) ClientApiConverter.toClientApi(edge, workspaceId, authorizations);
    }
}
