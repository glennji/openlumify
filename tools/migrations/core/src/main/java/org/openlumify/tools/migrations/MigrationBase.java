package org.openlumify.tools.migrations;

import org.vertexium.Graph;
import org.vertexium.GraphFactory;
import org.openlumify.core.cmdline.CommandLineTool;
import org.openlumify.core.config.Configuration;
import org.openlumify.core.exception.OpenLumifyException;

import static org.openlumify.core.bootstrap.OpenLumifyBootstrap.GRAPH_METADATA_VISALLO_GRAPH_VERSION_KEY;

public abstract class MigrationBase extends CommandLineTool {
    private Graph graph = null;

    @Override
    public final int run(String[] args, boolean initFramework) throws Exception {
        return super.run(args, initFramework);
    }

    @Override
    protected final int run() throws Exception {
        graph = getGraph();
        try {
            Object openlumifyGraphVersionObj = graph.getMetadata(GRAPH_METADATA_VISALLO_GRAPH_VERSION_KEY);
            if (openlumifyGraphVersionObj == null) {
                throw new OpenLumifyException("No graph metadata version set");
            } else if (openlumifyGraphVersionObj instanceof Integer) {
                Integer openlumifyGraphVersion = (Integer) openlumifyGraphVersionObj;
                if (getFinalGraphVersion().equals(openlumifyGraphVersion)) {
                    throw new OpenLumifyException("Migration has already completed. Graph version: " + openlumifyGraphVersion);
                } else if (!getNeededGraphVersion().equals(openlumifyGraphVersion)) {
                    throw new OpenLumifyException("Migration can only run from version " + getNeededGraphVersion() +
                            ". Current graph version = " + openlumifyGraphVersion);
                }
            } else {
                throw new OpenLumifyException("Unexpected value for graph version: " + openlumifyGraphVersionObj);
            }

            if (migrate(graph)) {
                graph.setMetadata(GRAPH_METADATA_VISALLO_GRAPH_VERSION_KEY, getFinalGraphVersion());
            }

            graph.flush();
            afterMigrate(graph);

            return 0;
        } finally {
            graph.shutdown();
        }
    }

    public abstract Integer getNeededGraphVersion();

    public abstract Integer getFinalGraphVersion();

    protected abstract boolean migrate(Graph graph);

    protected void afterMigrate(Graph graph) {
    }

    @Override
    public Graph getGraph() {
        if (graph == null) {
            GraphFactory factory = new GraphFactory();
            graph = factory.createGraph(getConfiguration().getSubset(Configuration.GRAPH_PROVIDER));
        }
        return graph;
    }
}
