package org.openlumify.core.bootstrap;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import com.google.inject.matcher.Matchers;
import org.vertexium.Graph;
import org.openlumify.core.cache.CacheService;
import org.openlumify.core.config.Configuration;
import org.openlumify.core.email.EmailRepository;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.geocoding.GeocoderRepository;
import org.openlumify.core.http.HttpRepository;
import org.openlumify.core.model.thumbnails.ThumbnailRepository;
import org.openlumify.core.model.directory.DirectoryRepository;
import org.openlumify.core.model.file.FileSystemRepository;
import org.openlumify.core.model.lock.LockRepository;
import org.openlumify.core.model.longRunningProcess.LongRunningProcessRepository;
import org.openlumify.core.model.notification.SystemNotificationRepository;
import org.openlumify.core.model.notification.UserNotificationRepository;
import org.openlumify.core.model.ontology.OntologyRepository;
import org.openlumify.core.model.search.SearchRepository;
import org.openlumify.core.model.user.*;
import org.openlumify.core.model.workQueue.WorkQueueRepository;
import org.openlumify.core.model.workspace.WorkspaceRepository;
import org.openlumify.core.security.ACLProvider;
import org.openlumify.core.security.AuditService;
import org.openlumify.core.security.VisibilityTranslator;
import org.openlumify.core.status.JmxMetricsManager;
import org.openlumify.core.status.MetricsManager;
import org.openlumify.core.time.TimeRepository;
import org.openlumify.core.trace.TraceRepository;
import org.openlumify.core.trace.Traced;
import org.openlumify.core.trace.TracedMethodInterceptor;
import org.openlumify.core.user.User;
import org.openlumify.core.util.ServiceLoaderUtil;
import org.openlumify.core.util.ShutdownService;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The OpenLumifyBootstrap is a Guice Module that configures itself by
 * discovering all available implementations of BootstrapBindingProvider
 * and invoking the addBindings() method.  If any discovered provider
 * cannot be instantiated, configuration of the Bootstrap Module will
 * fail and halt application initialization by throwing a BootstrapException.
 */
public class OpenLumifyBootstrap extends AbstractModule {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(OpenLumifyBootstrap.class);
    public static final String GRAPH_METADATA_VISALLO_GRAPH_VERSION_KEY = "openlumify.graph.version";
    private static final Integer GRAPH_METADATA_VISALLO_GRAPH_VERSION = 4;

    private static OpenLumifyBootstrap openlumifyBootstrap;

    public synchronized static OpenLumifyBootstrap bootstrap(final Configuration configuration) {
        if (openlumifyBootstrap == null) {
            LOGGER.debug("Initializing OpenLumifyBootstrap with Configuration:\n%s", configuration);
            openlumifyBootstrap = new OpenLumifyBootstrap(configuration);
        }
        return openlumifyBootstrap;
    }

    /**
     * Get a ModuleMaker that will return the OpenLumifyBootstrap, initializing it with
     * the provided Configuration if it has not already been created.
     *
     * @param configuration the OpenLumify configuration
     * @return a ModuleMaker for use with the InjectHelper
     */
    public static InjectHelper.ModuleMaker bootstrapModuleMaker(final Configuration configuration) {
        return new InjectHelper.ModuleMaker() {
            @Override
            public Module createModule() {
                return OpenLumifyBootstrap.bootstrap(configuration);
            }

            @Override
            public Configuration getConfiguration() {
                return configuration;
            }
        };
    }

    /**
     * The OpenLumify Configuration.
     */
    private final Configuration configuration;

    /**
     * Create a OpenLumifyBootstrap with the provided Configuration.
     *
     * @param config the configuration for this bootstrap
     */
    private OpenLumifyBootstrap(final Configuration config) {
        this.configuration = config;
    }

    @Override
    protected void configure() {
        LOGGER.info("Configuring OpenLumifyBootstrap.");

        checkNotNull(configuration, "configuration cannot be null");
        bind(Configuration.class).toInstance(configuration);

        LOGGER.debug("binding %s", JmxMetricsManager.class.getName());
        MetricsManager metricsManager = new JmxMetricsManager();
        bind(MetricsManager.class).toInstance(metricsManager);

        bindInterceptor(Matchers.any(), Matchers.annotatedWith(Traced.class), new TracedMethodInterceptor());

        bind(TraceRepository.class)
                .toProvider(OpenLumifyBootstrap.getConfigurableProvider(configuration, Configuration.TRACE_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(Graph.class)
                .toProvider(getGraphProvider(configuration, Configuration.GRAPH_PROVIDER))
                .in(Scopes.SINGLETON);
        bind(LockRepository.class)
                .toProvider(OpenLumifyBootstrap.getConfigurableProvider(configuration, Configuration.LOCK_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(WorkQueueRepository.class)
                .toProvider(OpenLumifyBootstrap.getConfigurableProvider(configuration, Configuration.WORK_QUEUE_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(LongRunningProcessRepository.class)
                .toProvider(OpenLumifyBootstrap.getConfigurableProvider(configuration, Configuration.LONG_RUNNING_PROCESS_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(DirectoryRepository.class)
                .toProvider(OpenLumifyBootstrap.getConfigurableProvider(configuration, Configuration.DIRECTORY_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(VisibilityTranslator.class)
                .toProvider(OpenLumifyBootstrap.getConfigurableProvider(configuration, Configuration.VISIBILITY_TRANSLATOR))
                .in(Scopes.SINGLETON);
        bind(UserRepository.class)
                .toProvider(OpenLumifyBootstrap.getConfigurableProvider(configuration, Configuration.USER_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(UserSessionCounterRepository.class)
                .toProvider(OpenLumifyBootstrap.getConfigurableProvider(configuration, Configuration.USER_SESSION_COUNTER_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(SearchRepository.class)
                .toProvider(OpenLumifyBootstrap.getConfigurableProvider(configuration, Configuration.SEARCH_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(WorkspaceRepository.class)
                .toProvider(OpenLumifyBootstrap.getConfigurableProvider(configuration, Configuration.WORKSPACE_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(GraphAuthorizationRepository.class)
                .toProvider(OpenLumifyBootstrap.getConfigurableProvider(configuration, Configuration.GRAPH_AUTHORIZATION_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(OntologyRepository.class)
                .toProvider(OpenLumifyBootstrap.getConfigurableProvider(configuration, Configuration.ONTOLOGY_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(HttpRepository.class)
                .toProvider(OpenLumifyBootstrap.getConfigurableProvider(configuration, Configuration.HTTP_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(GeocoderRepository.class)
                .toProvider(OpenLumifyBootstrap.getConfigurableProvider(configuration, Configuration.GEOCODER_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(EmailRepository.class)
                .toProvider(OpenLumifyBootstrap.getConfigurableProvider(configuration, Configuration.EMAIL_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(ACLProvider.class)
                .toProvider(OpenLumifyBootstrap.getConfigurableProvider(configuration, Configuration.ACL_PROVIDER_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(FileSystemRepository.class)
                .toProvider(OpenLumifyBootstrap.getConfigurableProvider(configuration, Configuration.FILE_SYSTEM_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(AuthorizationRepository.class)
                .toProvider(OpenLumifyBootstrap.getConfigurableProvider(configuration, Configuration.AUTHORIZATION_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(PrivilegeRepository.class)
                .toProvider(OpenLumifyBootstrap.getConfigurableProvider(configuration, Configuration.PRIVILEGE_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(CacheService.class)
                .toProvider(OpenLumifyBootstrap.getConfigurableProvider(configuration, Configuration.CACHE_SERVICE))
                .in(Scopes.SINGLETON);
        bind(AuditService.class)
                .toProvider(OpenLumifyBootstrap.getConfigurableProvider(configuration, Configuration.AUDIT_SERVICE))
                .in(Scopes.SINGLETON);
        bind(TimeRepository.class)
                .toInstance(new TimeRepository());
        bind(UserNotificationRepository.class)
                .toProvider(OpenLumifyBootstrap.getConfigurableProvider(configuration, Configuration.USER_NOTIFICATION_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(SystemNotificationRepository.class)
                .toProvider(OpenLumifyBootstrap.getConfigurableProvider(configuration, Configuration.SYSTEM_NOTIFICATION_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(ThumbnailRepository.class)
                .toProvider(OpenLumifyBootstrap.getConfigurableProvider(configuration, Configuration.THUMBNAIL_REPOSITORY))
                .in(Scopes.SINGLETON);
        injectProviders();
    }

    private Provider<? extends Graph> getGraphProvider(Configuration configuration, String configurationPrefix) {
        // TODO change to use org.vertexium.GraphFactory
        String graphClassName = configuration.get(configurationPrefix, null);
        if (graphClassName == null) {
            throw new OpenLumifyException("Could not find graph configuration: " + configurationPrefix);
        }
        final Map<String, String> configurationSubset = configuration.getSubset(configurationPrefix);

        final Class<?> graphClass;
        try {
            LOGGER.debug("Loading graph class \"%s\"", graphClassName);
            graphClass = Class.forName(graphClassName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not find graph class with name: " + graphClassName, e);
        }

        final Method createMethod;
        try {
            createMethod = graphClass.getDeclaredMethod("create", Map.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Could not find create(Map) method on class: " + graphClass.getName(), e);
        }

        return (Provider<Graph>) () -> {
            Graph g;
            try {
                LOGGER.debug("creating graph");
                g = (Graph) createMethod.invoke(null, configurationSubset);
            } catch (Exception e) {
                LOGGER.error("Could not create graph %s", graphClass.getName(), e);
                throw new OpenLumifyException("Could not create graph " + graphClass.getName(), e);
            }

            checkOpenLumifyGraphVersion(g);

            getShutdownService().register(new GraphShutdownListener(g));
            return g;
        };
    }

    private ShutdownService getShutdownService() {
        return InjectHelper.getInstance(ShutdownService.class);
    }

    public void checkOpenLumifyGraphVersion(Graph g) {
        Object openlumifyGraphVersionObj = g.getMetadata(GRAPH_METADATA_VISALLO_GRAPH_VERSION_KEY);
        if (openlumifyGraphVersionObj == null) {
            g.setMetadata(GRAPH_METADATA_VISALLO_GRAPH_VERSION_KEY, GRAPH_METADATA_VISALLO_GRAPH_VERSION);
        } else if (openlumifyGraphVersionObj instanceof Integer) {
            Integer openlumifyGraphVersion = (Integer) openlumifyGraphVersionObj;
            if (!GRAPH_METADATA_VISALLO_GRAPH_VERSION.equals(openlumifyGraphVersion)) {
                throw new OpenLumifyException("Invalid " + GRAPH_METADATA_VISALLO_GRAPH_VERSION_KEY + " expected " + GRAPH_METADATA_VISALLO_GRAPH_VERSION + " found " + openlumifyGraphVersion);
            }
        } else {
            throw new OpenLumifyException("Invalid " + GRAPH_METADATA_VISALLO_GRAPH_VERSION_KEY + " expected Integer found " + openlumifyGraphVersionObj.getClass().getName());
        }
    }

    private void injectProviders() {
        LOGGER.info("Running %s", BootstrapBindingProvider.class.getName());
        Iterable<BootstrapBindingProvider> bindingProviders = ServiceLoaderUtil.loadWithoutInjecting(BootstrapBindingProvider.class, configuration);
        for (BootstrapBindingProvider provider : bindingProviders) {
            LOGGER.debug("Configuring bindings from BootstrapBindingProvider: %s", provider.getClass().getName());
            provider.addBindings(this.binder(), configuration);
        }
    }

    public static void shutdown() {
        openlumifyBootstrap = null;
    }

    public static <T> Provider<? extends T> getConfigurableProvider(final Configuration config, final String key) {
        Class<? extends T> configuredClass = config.getClass(key);
        return configuredClass != null ? new ConfigurableProvider<>(configuredClass, config, key, null) : new NullProvider<>();
    }

    private static class NullProvider<T> implements Provider<T> {
        @Override
        public T get() {
            return null;
        }
    }

    private static class ConfigurableProvider<T> implements Provider<T> {
        private final Class<? extends T> clazz;
        private final Method initMethod;
        private final Object[] initMethodArgs;
        private final Configuration config;
        private final String keyPrefix;

        public ConfigurableProvider(final Class<? extends T> clazz, final Configuration config, String keyPrefix, final User user) {
            this.config = config;
            this.keyPrefix = keyPrefix;
            Method init;
            Object[] initArgs = null;
            init = findInit(clazz, Configuration.class, User.class);
            if (init != null) {
                initArgs = new Object[]{config, user};
            } else {
                init = findInit(clazz, Map.class, User.class);
                if (init != null) {
                    initArgs = new Object[]{config.toMap(), user};
                } else {
                    init = findInit(clazz, Configuration.class);
                    if (init != null) {
                        initArgs = new Object[]{config};
                    } else {
                        init = findInit(clazz, Map.class);
                        if (init != null) {
                            initArgs = new Object[]{config.toMap()};
                        }
                    }
                }
            }
            this.clazz = clazz;
            this.initMethod = init;
            this.initMethodArgs = initArgs;
        }

        private Method findInit(Class<? extends T> target, Class<?>... paramTypes) {
            try {
                return target.getMethod("init", paramTypes);
            } catch (NoSuchMethodException ex) {
                return null;
            } catch (SecurityException ex) {
                List<String> paramNames = new ArrayList<>();
                for (Class<?> pc : paramTypes) {
                    paramNames.add(pc.getSimpleName());
                }
                throw new OpenLumifyException(String.format("Error accessing init(%s) method in %s.", paramNames, clazz.getName()), ex);
            }
        }

        @Override
        public T get() {
            Throwable error;
            try {
                LOGGER.debug("creating %s", this.clazz.getName());
                T impl;
                if (InjectHelper.getInjector() != null) {
                    impl = InjectHelper.getInstance(this.clazz);
                } else {
                    Constructor<? extends T> constructor = this.clazz.getConstructor();
                    impl = constructor.newInstance();
                }
                if (initMethod != null) {
                    initMethod.invoke(impl, initMethodArgs);
                }
                config.setConfigurables(impl, this.keyPrefix);
                return impl;
            } catch (IllegalAccessException iae) {
                LOGGER.error("Unable to access default constructor for %s", clazz.getName(), iae);
                error = iae;
            } catch (IllegalArgumentException iae) {
                LOGGER.error("Unable to initialize instance of %s.", clazz.getName(), iae);
                error = iae;
            } catch (InvocationTargetException ite) {
                LOGGER.error("Error initializing instance of %s.", clazz.getName(), ite);
                error = ite;
            } catch (NoSuchMethodException e) {
                LOGGER.error("Could not find constructor for %s.", clazz.getName(), e);
                error = e;
            } catch (InstantiationException e) {
                LOGGER.error("Could not create %s.", clazz.getName(), e);
                error = e;
            }
            throw new OpenLumifyException(String.format("Unable to initialize instance of %s", clazz.getName()), error);
        }
    }
}
