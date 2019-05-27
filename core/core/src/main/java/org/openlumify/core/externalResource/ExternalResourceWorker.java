package org.openlumify.core.externalResource;

import com.google.inject.Inject;
import org.openlumify.core.status.MetricEntry;
import org.openlumify.core.status.MetricsManager;
import org.openlumify.core.user.User;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;

import java.util.Collection;

public abstract class ExternalResourceWorker {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(ExternalResourceWorker.class);
    private MetricsManager metricsManager;

    protected void prepare(
            @SuppressWarnings("UnusedParameters") User user
    ) {

    }

    protected abstract void run() throws Exception;

    protected abstract void stop();

    @Inject
    public final void setMetricsManager(MetricsManager metricsManager) {
        this.metricsManager = metricsManager;
    }

    public MetricsManager getMetricsManager() {
        return metricsManager;
    }

    public abstract Collection<MetricEntry> getMetrics();
}
