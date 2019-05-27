package org.openlumify.core.process;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.core.config.Configurable;
import org.openlumify.core.config.Configuration;
import org.openlumify.core.model.longRunningProcess.LongRunningProcessRunner;
import org.openlumify.core.util.*;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class LongRunningProcessRunnerProcess implements OpenLumifyProcess, ShutdownListener {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(LongRunningProcessRunnerProcess.class);
    private final Configuration configuration;
    private final Config config;
    private final List<StoppableRunnable> stoppables = new ArrayList<>();

    public static class Config {
        @Configurable
        public int threadCount;
    }

    @Inject
    public LongRunningProcessRunnerProcess(Configuration configuration, ShutdownService shutdownService) {
        this.configuration = configuration;
        this.config = configuration.setConfigurables(new Config(), LongRunningProcessRunnerProcess.class.getName());
        shutdownService.register(this);
    }

    @Override
    public void startProcess(OpenLumifyProcessOptions options) {
        if (config.threadCount <= 0) {
            LOGGER.info("'threadCount' not configured or was 0");
            return;
        }

        stoppables.addAll(LongRunningProcessRunner.startThreaded(config.threadCount, configuration));
    }

    @Override
    public void shutdown() {
        stoppables.forEach(StoppableRunnable::stop);
    }
}
