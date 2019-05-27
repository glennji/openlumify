package org.openlumify.web.routes.element;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.webster.ParameterizedHandler;
import org.vertexium.Graph;
import org.openlumify.core.model.search.ElementSearchRunner;
import org.openlumify.core.model.search.VertexiumObjectSearchRunnerBase;
import org.openlumify.core.model.search.SearchRepository;
import org.openlumify.web.routes.vertex.VertexiumObjectSearchBase;

@Singleton
public class ElementSearch extends VertexiumObjectSearchBase implements ParameterizedHandler {
    @Inject
    public ElementSearch(Graph graph, SearchRepository searchRepository) {
        super(graph, (VertexiumObjectSearchRunnerBase) searchRepository.findSearchRunnerByUri(ElementSearchRunner.URI));
    }
}
