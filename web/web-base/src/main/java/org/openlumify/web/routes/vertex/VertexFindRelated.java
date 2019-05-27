package org.openlumify.web.routes.vertex;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.webster.ParameterizedHandler;
import org.openlumify.webster.annotations.Handle;
import org.vertexium.Authorizations;
import org.vertexium.Element;
import org.vertexium.Vertex;
import org.openlumify.core.model.search.SearchOptions;
import org.openlumify.core.model.search.SearchRepository;
import org.openlumify.core.model.search.VertexFindRelatedSearchResults;
import org.openlumify.core.model.search.VertexFindRelatedSearchRunner;
import org.openlumify.core.user.User;
import org.openlumify.core.util.ClientApiConverter;
import org.openlumify.web.clientapi.model.ClientApiElementFindRelatedResponse;
import org.openlumify.web.clientapi.model.ClientApiVertex;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;
import org.openlumify.web.routes.search.WebSearchOptionsFactory;

import javax.servlet.http.HttpServletRequest;

@Singleton
public class VertexFindRelated implements ParameterizedHandler {
    private final VertexFindRelatedSearchRunner searchRunner;

    @Inject
    public VertexFindRelated(SearchRepository searchRepository) {
        this.searchRunner =
                (VertexFindRelatedSearchRunner) searchRepository.findSearchRunnerByUri(VertexFindRelatedSearchRunner.URI);
    }

    @Handle
    public ClientApiElementFindRelatedResponse handle(
            HttpServletRequest request,
            @ActiveWorkspaceId String workspaceId,
            User user,
            Authorizations authorizations
    ) throws Exception {
        SearchOptions searchOptions = WebSearchOptionsFactory.create(request, workspaceId);
        return getVertices(searchOptions, user, authorizations);
    }

    /**
     * This is overridable so web plugins can modify the resulting set of vertices.
     */
    protected ClientApiElementFindRelatedResponse getVertices(
            SearchOptions searchOptions,
            User user,
            Authorizations authorizations
    ) {
        VertexFindRelatedSearchResults results = this.searchRunner.run(searchOptions, user, authorizations);
        ClientApiElementFindRelatedResponse response = new ClientApiElementFindRelatedResponse();
        for (Element element : results.getVertexiumObjects()) {
            Vertex vertex = (Vertex) element;
            ClientApiVertex clientApiVertex = ClientApiConverter.toClientApiVertex(vertex, searchOptions.getWorkspaceId(), authorizations);
            response.getElements().add(clientApiVertex);
        }
        response.setCount(results.getCount());
        return response;
    }

}

