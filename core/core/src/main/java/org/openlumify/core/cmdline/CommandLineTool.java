package org.openlumify.core.cmdline;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.inject.Inject;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.openlumify.core.bootstrap.InjectHelper;
import org.openlumify.core.bootstrap.OpenLumifyBootstrap;
import org.openlumify.core.config.Configuration;
import org.openlumify.core.config.ConfigurationLoader;
import org.openlumify.core.model.lock.LockRepository;
import org.openlumify.core.model.ontology.OntologyRepository;
import org.openlumify.core.model.user.AuthorizationRepository;
import org.openlumify.core.model.user.PrivilegeRepository;
import org.openlumify.core.model.user.UserRepository;
import org.openlumify.core.model.user.UserSessionCounterRepository;
import org.openlumify.core.model.workQueue.WorkQueueRepository;
import org.openlumify.core.security.VisibilityTranslator;
import org.openlumify.core.user.User;
import org.openlumify.core.util.ShutdownService;
import org.openlumify.core.util.VersionUtil;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;

public abstract class CommandLineTool {
    public static final String THREAD_NAME = "OpenLumify CLI";
    protected OpenLumifyLogger LOGGER;
    public static final boolean DEFAULT_INIT_FRAMEWORK = true;
    private Configuration configuration;
    private boolean willExit = false;
    private UserRepository userRepository;
    private AuthorizationRepository authorizationRepository;
    private Authorizations authorizations;
    private PrivilegeRepository privilegeRepository;
    private LockRepository lockRepository;
    private User user;
    private Graph graph;
    private WorkQueueRepository workQueueRepository;
    private OntologyRepository ontologyRepository;
    private VisibilityTranslator visibilityTranslator;
    private ShutdownService shutdownService;
    private UserSessionCounterRepository userSessionCounterRepository;
    private JCommander jCommander;
    private boolean frameworkInitialized;

    @Parameter(names = {"--help", "-h"}, description = "Print help", help = true)
    private boolean help;

    @Parameter(names = {"--version"}, description = "Print version")
    private boolean version;

    public int run(String[] args) throws Exception {
        return run(args, DEFAULT_INIT_FRAMEWORK);
    }

    public int run(String[] args, boolean initFramework) throws Exception {
        try {
            LOGGER = OpenLumifyLoggerFactory.getLogger(CommandLineTool.class, "cli");
            final Thread mainThread = Thread.currentThread();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                willExit = true;

                if (frameworkInitialized) {
                    shutdown();
                }

                try {
                    mainThread.join(1000);
                } catch (InterruptedException e) {
                    // nothing useful to do here
                }
            }));

            try {
                jCommander = parseArguments(args);
                if (jCommander == null) {
                    return -1;
                }
                if (help) {
                    printHelp(jCommander);
                    return -1;
                }
                if (version) {
                    VersionUtil.printVersion();
                    return -1;
                }
            } catch (ParameterException ex) {
                System.err.println(ex.getMessage());
                return -1;
            }

            if (initFramework) {
                initializeFramework();
            }

            int result = -1;
            result = run();
            LOGGER.debug("command result: %d", result);
            return result;
        } finally {
            if (frameworkInitialized) {
                shutdown();
            }
        }
    }

    protected void initializeFramework() {
        InjectHelper.inject(this, OpenLumifyBootstrap.bootstrapModuleMaker(getConfiguration()), getConfiguration());
        frameworkInitialized = true;
    }

    protected void printHelp(JCommander j) {
        j.usage();
    }

    protected JCommander parseArguments(String[] args) {
        return new JCommander(this, args);
    }

    protected void shutdown() {
        getShutdownService().shutdown();
    }

    protected abstract int run() throws Exception;

    protected Configuration getConfiguration() {
        if (configuration == null) {
            configuration = ConfigurationLoader.load();
        }
        return configuration;
    }

    protected User getUser() {
        if (this.user == null) {
            this.user = userRepository.getSystemUser();
        }
        return this.user;
    }

    protected Authorizations getAuthorizations() {
        if (this.authorizations == null) {
            this.authorizations = getAuthorizationRepository().getGraphAuthorizations(getUser());
        }
        return this.authorizations;
    }

    protected boolean willExit() {
        return willExit;
    }

    @Inject
    public void setLockRepository(LockRepository lockRepository) {
        this.lockRepository = lockRepository;
    }

    @Inject
    public final void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Inject
    public void setAuthorizationRepository(AuthorizationRepository authorizationRepository) {
        this.authorizationRepository = authorizationRepository;
    }

    @Inject
    public void setPrivilegeRepository(PrivilegeRepository privilegeRepository) {
        this.privilegeRepository = privilegeRepository;
    }

    @Inject
    public final void setGraph(Graph graph) {
        this.graph = graph;
    }

    @Inject
    public final void setWorkQueueRepository(WorkQueueRepository workQueueRepository) {
        this.workQueueRepository = workQueueRepository;
    }

    @Inject
    public final void setOntologyRepository(OntologyRepository ontologyRepository) {
        this.ontologyRepository = ontologyRepository;
    }

    @Inject
    public void setShutdownService(ShutdownService shutdownService) {
        this.shutdownService = shutdownService;
    }

    @Inject
    public void setVisibilityTranslator(VisibilityTranslator visibilityTranslator) {
        this.visibilityTranslator = visibilityTranslator;
    }

    @Inject
    public void setUserSessionCounterRepository(UserSessionCounterRepository userSessionCounterRepository) {
        this.userSessionCounterRepository = userSessionCounterRepository;
    }

    public Graph getGraph() {
        return graph;
    }

    public WorkQueueRepository getWorkQueueRepository() {
        return workQueueRepository;
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }

    public AuthorizationRepository getAuthorizationRepository() {
        return authorizationRepository;
    }

    public PrivilegeRepository getPrivilegeRepository() {
        return privilegeRepository;
    }

    public OntologyRepository getOntologyRepository() {
        return ontologyRepository;
    }

    public VisibilityTranslator getVisibilityTranslator() {
        return visibilityTranslator;
    }

    public ShutdownService getShutdownService() {
        return shutdownService;
    }

    public UserSessionCounterRepository getUserSessionCounterRepository() {
        return userSessionCounterRepository;
    }

    public static void main(CommandLineTool commandLineTool, String[] args, boolean initFramework) throws Exception {
        int res = commandLineTool.run(args, initFramework);
        if (res != 0) {
            System.exit(res);
        }
    }

    public static void main(CommandLineTool commandLineTool, String[] args) throws Exception {
        Thread.currentThread().setName(THREAD_NAME);
        main(commandLineTool, args, DEFAULT_INIT_FRAMEWORK);
    }
}
