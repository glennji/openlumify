package org.openlumify.web.routes.edge;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.webster.ParameterizedHandler;
import org.vertexium.Graph;
import org.openlumify.core.model.search.EdgeSearchRunner;
import org.openlumify.core.model.search.VertexiumObjectSearchRunnerBase;
import org.openlumify.core.model.search.SearchRepository;
import org.openlumify.web.routes.vertex.VertexiumObjectSearchBase;

@Singleton
public class EdgeSearch extends VertexiumObjectSearchBase implements ParameterizedHandler {
    @Inject
    public EdgeSearch(Graph graph, SearchRepository searchRepository) {
        super(graph, (VertexiumObjectSearchRunnerBase) searchRepository.findSearchRunnerByUri(EdgeSearchRunner.URI));
    }
}
