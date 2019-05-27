package org.openlumify.core.process;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.core.config.Configuration;
import org.openlumify.core.externalResource.ExternalResourceRunner;
import org.openlumify.core.util.ShutdownListener;
import org.openlumify.core.util.ShutdownService;

@Singleton
public class ExternalResourceRunnerProcess implements OpenLumifyProcess, ShutdownListener {
    private final Configuration configuration;
    private ExternalResourceRunner resourceRunner;

    @Inject
    public ExternalResourceRunnerProcess(
            Configuration configuration,
            ShutdownService shutdownService
    ) {
        this.configuration = configuration;
        shutdownService.register(this);
    }

    @Override
    public void startProcess(OpenLumifyProcessOptions options) {
        resourceRunner = new ExternalResourceRunner(configuration, options.getUser());
        resourceRunner.startAll();
    }

    @Override
    public void shutdown() {
        if (resourceRunner != null) {
            resourceRunner.shutdown();
        }
    }
}
