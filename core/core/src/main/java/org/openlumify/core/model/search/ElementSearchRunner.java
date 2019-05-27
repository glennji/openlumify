package org.openlumify.core.model.search;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.json.JSONArray;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.VertexiumObjectType;
import org.vertexium.query.Query;
import org.openlumify.core.config.Configuration;
import org.openlumify.core.model.directory.DirectoryRepository;
import org.openlumify.core.model.ontology.OntologyRepository;
import org.openlumify.core.user.User;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;

import java.util.EnumSet;

@Singleton
public class ElementSearchRunner extends VertexiumObjectSearchRunnerWithRelatedBase {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(ElementSearchRunner.class);
    public static final String URI = "/element/search";

    @Inject
    public ElementSearchRunner(
            OntologyRepository ontologyRepository,
            Graph graph,
            Configuration configuration,
            DirectoryRepository directoryRepository
    ) {
        super(ontologyRepository, graph, configuration, directoryRepository);
    }

    @Override
    protected EnumSet<VertexiumObjectType> getResultType() {
        return EnumSet.of(VertexiumObjectType.EDGE, VertexiumObjectType.VERTEX);
    }

    @Override
    public String getUri() {
        return URI;
    }

    @Override
    protected QueryAndData getQuery(SearchOptions searchOptions, Authorizations authorizations) {
        JSONArray filterJson = getFilterJson(searchOptions, searchOptions.getWorkspaceId());
        String queryString = searchOptions.getRequiredParameter("q", String.class);
        LOGGER.debug("search %s\n%s", queryString, filterJson.toString(2));

        Query graphQuery = query(queryString, authorizations);

        return new QueryAndData(graphQuery);
    }

    private Query query(String query, Authorizations authorizations) {
        return getGraph().query(query, authorizations);
    }
}
