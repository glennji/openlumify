package org.openlumify.core.util;

import com.google.common.collect.Lists;
import com.google.inject.Singleton;
import org.openlumify.core.bootstrap.InjectHelper;
import org.openlumify.core.bootstrap.OpenLumifyBootstrap;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Singleton
public class ShutdownService {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(ShutdownService.class);
    private LinkedHashSet<ShutdownListener> shutdownListeners = new LinkedHashSet<>();
    private boolean shutdownCalled;

    public synchronized void shutdown() {
        if (shutdownCalled) {
            LOGGER.debug("shutdown already called");
            return;
        }
        shutdownCalled = true;

        // shutdown in reverse order to better handle dependencies
        List<ShutdownListener> shutdownListenersList = Lists.reverse(new ArrayList<>(shutdownListeners));
        for (ShutdownListener shutdownListener : shutdownListenersList) {
            try {
                LOGGER.info("Shutdown: " + shutdownListener.getClass().getName());
                shutdownListener.shutdown();
            } catch (Exception e) {
                LOGGER.error("Unable to shutdown: " + shutdownListener.getClass().getName(), e);
            }
        }

        LOGGER.info("Shutdown: InjectHelper");
        InjectHelper.shutdown();

        LOGGER.info("Shutdown: OpenLumifyBootstrap");
        OpenLumifyBootstrap.shutdown();
    }

    /**
     * Classes that implement {@link ShutdownListener} call this method to be notified of
     * a OpenLumify shutdown. We can not use the service locator pattern to find shutdown listeners because that
     * may cause an inadvertent initialization of that class.
     */
    public void register(ShutdownListener shutdownListener) {
        this.shutdownListeners.add(shutdownListener);
    }
}
