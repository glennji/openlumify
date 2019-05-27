package org.openlumify.core.process;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.core.config.Configurable;
import org.openlumify.core.config.Configuration;
import org.openlumify.core.ingest.graphProperty.GraphPropertyRunner;
import org.openlumify.core.util.*;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class GraphPropertyRunnerProcess implements OpenLumifyProcess, ShutdownListener {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(GraphPropertyRunnerProcess.class);
    private final Config config;
    private final List<StoppableRunnable> stoppables = new ArrayList<>();

    public static class Config {
        @Configurable
        public int threadCount;
    }

    @Inject
    public GraphPropertyRunnerProcess(Configuration configuration, ShutdownService shutdownService) {
        this(configuration.setConfigurables(new Config(), GraphPropertyRunnerProcess.class.getName()));
        shutdownService.register(this);
    }

    public GraphPropertyRunnerProcess(Config config) {
        this.config = config;
    }

    @Override
    public void startProcess(OpenLumifyProcessOptions options) {
        if (config.threadCount <= 0) {
            LOGGER.info("'threadCount' not configured or was 0");
            return;
        }

        stoppables.addAll(GraphPropertyRunner.startThreaded(config.threadCount, options.getUser()));
    }

    @Override
    public void shutdown() {
        stoppables.forEach(StoppableRunnable::stop);
    }
}
