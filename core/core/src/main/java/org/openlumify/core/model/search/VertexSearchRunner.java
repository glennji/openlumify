package org.openlumify.core.model.search;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.vertexium.Graph;
import org.vertexium.VertexiumObjectType;
import org.openlumify.core.config.Configuration;
import org.openlumify.core.model.directory.DirectoryRepository;
import org.openlumify.core.model.ontology.OntologyRepository;

import java.util.EnumSet;

@Singleton
public class VertexSearchRunner extends VertexiumObjectSearchRunnerWithRelatedBase {
    public static final String URI = "/vertex/search";

    @Inject
    public VertexSearchRunner(
            OntologyRepository ontologyRepository,
            Graph graph,
            Configuration configuration,
            DirectoryRepository directoryRepository
    ) {
        super(ontologyRepository, graph, configuration, directoryRepository);
    }

    @Override
    protected EnumSet<VertexiumObjectType> getResultType() {
        return EnumSet.of(VertexiumObjectType.VERTEX);
    }

    @Override
    public String getUri() {
        return URI;
    }
}
