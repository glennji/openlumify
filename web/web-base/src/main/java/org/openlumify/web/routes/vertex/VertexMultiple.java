package org.openlumify.web.routes.vertex;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.openlumify.core.exception.OpenLumifyAccessDeniedException;
import org.openlumify.core.model.user.AuthorizationRepository;
import org.openlumify.core.model.workspace.WorkspaceRepository;
import org.openlumify.core.user.User;
import org.openlumify.core.util.ClientApiConverter;
import org.openlumify.web.clientapi.model.ClientApiVertex;
import org.openlumify.web.clientapi.model.ClientApiVertexMultipleResponse;
import org.openlumify.web.parameterProviders.OpenLumifyBaseParameterProvider;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Optional;
import org.visallo.webster.annotations.Required;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class VertexMultiple implements ParameterizedHandler {
    private final Graph graph;
    private final WorkspaceRepository workspaceRepository;
    private final AuthorizationRepository authorizationRepository;

    @Inject
    public VertexMultiple(
            Graph graph,
            WorkspaceRepository workspaceRepository,
            AuthorizationRepository authorizationRepository
    ) {
        this.graph = graph;
        this.workspaceRepository = workspaceRepository;
        this.authorizationRepository = authorizationRepository;
    }

    @Handle
    public ClientApiVertexMultipleResponse handle(
            HttpServletRequest request,
            @Required(name = "vertexIds[]") String[] vertexIdsParam,
            @Optional(name = "fallbackToPublic", defaultValue = "false") boolean fallbackToPublic,
            @Optional(name = "includeAncillary", defaultValue = "false") boolean includeAncillary,
            User user
    ) throws Exception {
        ClientApiVertexMultipleResponse result = new ClientApiVertexMultipleResponse();
        String workspaceId = null;

        try {
            workspaceId = OpenLumifyBaseParameterProvider.getActiveWorkspaceIdOrDefault(request, workspaceRepository);
            result.setRequiredFallback(false);
        } catch (OpenLumifyAccessDeniedException ex) {
            if (fallbackToPublic) {
                result.setRequiredFallback(true);
            } else {
                throw ex;
            }
        }

        List<String> auths = new ArrayList<>();
        if (workspaceId != null) {
            auths.add(workspaceId);
        }
        if (includeAncillary) {
            auths.add(WorkspaceRepository.VISIBILITY_PRODUCT_STRING);
        }

        Authorizations authorizations = authorizationRepository.getGraphAuthorizations(user, auths.toArray(new String[]{}));

        Iterable<Vertex> graphVertices = graph.getVertices(
                Sets.newHashSet(vertexIdsParam),
                ClientApiConverter.SEARCH_FETCH_HINTS,
                authorizations
        );
        
        for (Vertex v : graphVertices) {
            ClientApiVertex vertex = ClientApiConverter.toClientApiVertex(
                    v,
                    workspaceId,
                    authorizations
            );

            result.getVertices().add(vertex);
        }

        return result;
    }
}
