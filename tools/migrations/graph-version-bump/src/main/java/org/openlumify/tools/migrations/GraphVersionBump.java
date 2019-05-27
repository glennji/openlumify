package org.openlumify.tools.migrations;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.vertexium.*;
import org.openlumify.core.bootstrap.OpenLumifyBootstrap;
import org.openlumify.core.cmdline.CommandLineTool;
import org.openlumify.core.config.Configuration;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;

@Parameters(commandDescription = "Update OpenLumify metadata graph version")
public class GraphVersionBump extends CommandLineTool {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(GraphVersionBump.class);
    private static final String VISALLO_GRAPH_VERSION = "openlumify.graph.version";
    private Graph graph = null;

    @Parameter(required = true, names = {"--graph-version"}, description = "Specify the metadata graph version to set.")
    private Integer toVersion;

    public static void main(String[] args) throws Exception {
        CommandLineTool.main(new GraphVersionBump(), args, false);
    }

    @Override
    protected int run() throws Exception {
        OpenLumifyBootstrap bootstrap = OpenLumifyBootstrap.bootstrap(getConfiguration());
        graph = getGraph();

        try {
            Object openlumifyGraphVersionObj = graph.getMetadata(VISALLO_GRAPH_VERSION);

            if (openlumifyGraphVersionObj == null) {
                throw new OpenLumifyException("No graph metadata version set");
            } else if (openlumifyGraphVersionObj instanceof Integer) {
                Integer openlumifyGraphVersion = (Integer) openlumifyGraphVersionObj;
                if (toVersion.equals(openlumifyGraphVersion)) {
                    return 0;
                }
            }

            graph.setMetadata(VISALLO_GRAPH_VERSION, toVersion);
            return 0;
        } finally {
            graph.shutdown();
        }
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

