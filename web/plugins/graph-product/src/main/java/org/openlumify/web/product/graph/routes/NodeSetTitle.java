package org.openlumify.web.product.graph.routes;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Edge;
import org.vertexium.Graph;
import org.vertexium.mutation.ElementMutation;
import org.openlumify.core.exception.OpenLumifyAccessDeniedException;
import org.openlumify.core.model.user.AuthorizationRepository;
import org.openlumify.core.model.workQueue.WorkQueueRepository;
import org.openlumify.core.model.workspace.Workspace;
import org.openlumify.core.model.workspace.WorkspaceRepository;
import org.openlumify.core.model.workspace.product.WorkProductServiceHasElementsBase;
import org.openlumify.core.user.User;
import org.openlumify.web.OpenLumifyResponse;
import org.openlumify.web.clientapi.model.ClientApiSuccess;
import org.openlumify.web.clientapi.model.ClientApiWorkspace;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;
import org.openlumify.web.parameterProviders.SourceGuid;
import org.openlumify.web.product.graph.GraphProductOntology;
import org.openlumify.web.product.graph.GraphWorkProductService;

@Singleton
public class NodeSetTitle implements ParameterizedHandler {
    private final Graph graph;
    private final WorkspaceRepository workspaceRepository;
    private final WorkQueueRepository workQueueRepository;
    private final AuthorizationRepository authorizationRepository;

    @Inject
    public NodeSetTitle(
            Graph graph,
            WorkspaceRepository workspaceRepository,
            WorkQueueRepository workQueueRepository,
            AuthorizationRepository authorizationRepository
    ) {
        this.graph = graph;
        this.workspaceRepository = workspaceRepository;
        this.workQueueRepository = workQueueRepository;
        this.authorizationRepository = authorizationRepository;
    }

    @Handle
    public ClientApiSuccess handle(
            @Required(name = "compoundNodeId") String compoundNodeId,
            @Required(name = "title") String title,
            @Required(name = "productId") String productId,
            @ActiveWorkspaceId String workspaceId,
            @SourceGuid String sourceGuid,
            User user
    ) throws Exception {
        if (!workspaceRepository.hasWritePermissions(workspaceId, user)) {
            throw new OpenLumifyAccessDeniedException(
                    "user " + user.getUserId() + " does not have write access to workspace " + workspaceId,
                    user,
                    workspaceId
            );
        }

        Authorizations authorizations = authorizationRepository.getGraphAuthorizations(
                user,
                WorkspaceRepository.VISIBILITY_STRING,
                workspaceId
        );

        String edgeId = WorkProductServiceHasElementsBase.getEdgeId(productId, compoundNodeId);
        Edge productEdge = graph.getEdge(edgeId, authorizations);
        ElementMutation<Edge> m = productEdge.prepareMutation();
        GraphProductOntology.NODE_TITLE.setProperty(m, title, GraphWorkProductService.VISIBILITY.getVisibility());
        m.save(authorizations);
        graph.flush();

        Workspace workspace = workspaceRepository.findById(workspaceId, user);
        ClientApiWorkspace clientApiWorkspace = workspaceRepository.toClientApi(workspace, user, authorizations);

        workQueueRepository.broadcastWorkProductChange(productId, clientApiWorkspace, user, sourceGuid);

        return OpenLumifyResponse.SUCCESS;
    }
}
