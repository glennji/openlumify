package org.openlumify.core.bootstrap;

import org.openlumify.core.config.Configuration;
import com.google.inject.Binder;

/**
 * A BootstrapBindingProvider can add Guice bindings to the OpenLumify Bootstrap Module.
 * Implementations are automatically discovered by the OpenLumify Bootstrapper and will be
 * instantiated using an empty constructor.
 */
public interface BootstrapBindingProvider {
    /**
     * Add the bindings defined by this BootstrapBindingProvider to
     * the OpenLumify Bootstrap module.
     * @param binder the Binder that configures the Bootstrapper
     * @param configuration the OpenLumify Configuration
     */
    void addBindings(final Binder binder, final Configuration configuration);
}
