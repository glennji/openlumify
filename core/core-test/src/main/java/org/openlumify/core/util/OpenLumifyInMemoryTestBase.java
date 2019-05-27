package org.openlumify.core.util;

import org.json.JSONObject;
import org.junit.Before;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.TextIndexHint;
import org.vertexium.Vertex;
import org.vertexium.inmemory.InMemoryGraph;
import org.openlumify.core.action.Action;
import org.openlumify.core.action.ActionRepository;
import org.openlumify.core.cache.CacheService;
import org.openlumify.core.cache.NopCacheService;
import org.openlumify.core.config.Configuration;
import org.openlumify.core.config.HashMapConfigurationLoader;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.formula.FormulaEvaluator;
import org.openlumify.core.model.WorkQueueNames;
import org.openlumify.core.model.file.ClassPathFileSystemRepository;
import org.openlumify.core.model.file.FileSystemRepository;
import org.openlumify.core.model.graph.GraphRepository;
import org.openlumify.core.model.lock.LockRepository;
import org.openlumify.core.model.lock.NonLockingLockRepository;
import org.openlumify.core.model.longRunningProcess.LongRunningProcessRepository;
import org.openlumify.core.model.notification.InMemorySystemNotificationRepository;
import org.openlumify.core.model.notification.InMemoryUserNotificationRepository;
import org.openlumify.core.model.notification.SystemNotificationRepository;
import org.openlumify.core.model.notification.UserNotificationRepository;
import org.openlumify.core.model.ontology.OntologyRepository;
import org.openlumify.core.model.termMention.TermMentionRepository;
import org.openlumify.core.model.thumbnails.InMemoryThumbnailRepository;
import org.openlumify.core.model.thumbnails.ThumbnailRepository;
import org.openlumify.core.model.user.*;
import org.openlumify.core.model.workQueue.TestWorkQueueRepository;
import org.openlumify.core.model.workQueue.WorkQueueRepository;
import org.openlumify.core.model.workspace.*;
import org.openlumify.core.model.workspace.product.GetExtendedDataParams;
import org.openlumify.core.model.workspace.product.WorkProductExtendedData;
import org.openlumify.core.model.workspace.product.WorkProductService;
import org.openlumify.core.security.AuditService;
import org.openlumify.core.security.DirectVisibilityTranslator;
import org.openlumify.core.security.LoggingAuditService;
import org.openlumify.core.security.VisibilityTranslator;
import org.openlumify.core.time.MockTimeRepository;
import org.openlumify.core.time.TimeRepository;
import org.openlumify.core.user.User;
import org.openlumify.vertexium.model.longRunningProcess.VertexiumLongRunningProcessRepository;
import org.openlumify.vertexium.model.ontology.InMemoryOntologyRepository;
import org.openlumify.vertexium.model.user.InMemoryUser;
import org.openlumify.vertexium.model.user.VertexiumUserRepository;
import org.openlumify.vertexium.model.workspace.VertexiumWorkspaceRepository;

import java.util.*;

public abstract class OpenLumifyInMemoryTestBase {
    public static final String GRAPH_WORK_PRODUCT_KIND = "org.openlumify.web.product.graph.GraphWorkProduct";
    public static final String MAP_WORK_PRODUCT_KIND = "org.openlumify.web.product.map.MapWorkProduct";
    private WorkspaceRepository workspaceRepository;
    private Graph graph;
    private GraphRepository graphRepository;
    private Configuration configuration;
    private UserRepository userRepository;
    private GraphAuthorizationRepository graphAuthorizationRepository;
    private LockRepository lockRepository;
    private VisibilityTranslator visibilityTranslator;
    private OntologyRepository ontologyRepository;
    private WorkQueueRepository workQueueRepository;
    private AuthorizationRepository authorizationRepository;
    private WorkspaceDiffHelper workspaceDiffHelper;
    private FormulaEvaluator formulaEvaluator;
    private TermMentionRepository termMentionRepository;
    private UserNotificationRepository userNotificationRepository;
    private SystemNotificationRepository systemNotificationRepository;
    private UserSessionCounterRepository userSessionCounterRepository;
    private TimeRepository timeRepository;
    private PrivilegeRepository privilegeRepository;
    private FileSystemRepository fileSystemRepository;
    private LongRunningProcessRepository longRunningProcessRepository;
    private WorkQueueNames workQueueNames;
    private WorkspaceHelper workspaceHelper;
    private CacheService cacheService;
    private Map configurationMap;
    private ThumbnailRepository thumbnailRepository;
    private AuditService auditService;
    private User user;
    private ActionRepository actionRepository;
    private WorkProductService graphWorkProduct;
    private WorkProductService mapWorkProduct;

    @Before
    public void before() throws Exception {
        workspaceRepository = null;
        graph = null;
        graphRepository = null;
        configuration = null;
        userRepository = null;
        graphAuthorizationRepository = null;
        lockRepository = null;
        visibilityTranslator = null;
        ontologyRepository = null;
        workQueueRepository = null;
        authorizationRepository = null;
        workspaceDiffHelper = null;
        formulaEvaluator = null;
        termMentionRepository = null;
        userNotificationRepository = null;
        systemNotificationRepository = null;
        userSessionCounterRepository = null;
        timeRepository = null;
        privilegeRepository = null;
        fileSystemRepository = null;
        longRunningProcessRepository = null;
        workQueueNames = null;
        workspaceHelper = null;
        configurationMap = null;
        cacheService = null;
        thumbnailRepository = null;
        auditService = null;
        user = null;
        actionRepository = null;
        graphWorkProduct = null;
        mapWorkProduct = null;
    }

    protected WorkspaceRepository getWorkspaceRepository() {
        if (workspaceRepository != null) {
            return workspaceRepository;
        }
        workspaceRepository = new VertexiumWorkspaceRepository(
                getGraph(),
                getConfiguration(),
                getGraphRepository(),
                getUserRepository(),
                getGraphAuthorizationRepository(),
                getWorkspaceDiffHelper(),
                getLockRepository(),
                getVisibilityTranslator(),
                getTermMentionRepository(),
                getOntologyRepository(),
                getWorkQueueRepository(),
                getAuthorizationRepository()
        ) {
            @Override
            protected WorkProductService getWorkProductServiceByKind(String kind) {
                return OpenLumifyInMemoryTestBase.this.getWorkProductServiceByKind(kind);
            }

            @Override
            protected Collection<WorkspaceListener> getWorkspaceListeners() {
                return OpenLumifyInMemoryTestBase.this.getWorkspaceListeners();
            }
        };
        getGraph().defineProperty(WorkspaceProperties.TITLE.getPropertyName()).dataType(String.class).textIndexHint(TextIndexHint.ALL).define();
        return workspaceRepository;
    }

    protected Collection<WorkspaceListener> getWorkspaceListeners() {
        return new ArrayList<>();
    }

    protected TermMentionRepository getTermMentionRepository() {
        if (termMentionRepository != null) {
            return termMentionRepository;
        }
        termMentionRepository = new TermMentionRepository(
                getGraph(),
                getVisibilityTranslator(),
                getWorkQueueRepository(),
                getGraphAuthorizationRepository()
        );
        return termMentionRepository;
    }

    protected WorkspaceDiffHelper getWorkspaceDiffHelper() {
        if (workspaceDiffHelper != null) {
            return workspaceDiffHelper;
        }
        workspaceDiffHelper = new WorkspaceDiffHelper(
                getGraph(),
                getUserRepository(),
                getAuthorizationRepository(),
                getFormulaEvaluator()
        );
        return workspaceDiffHelper;
    }

    protected FormulaEvaluator getFormulaEvaluator() {
        if (formulaEvaluator != null) {
            return formulaEvaluator;
        }
        formulaEvaluator = new FormulaEvaluator(
                getConfiguration(),
                getOntologyRepository()
        );
        return formulaEvaluator;
    }

    protected AuthorizationRepository getAuthorizationRepository() {
        if (authorizationRepository != null) {
            return authorizationRepository;
        }
        authorizationRepository = new UserPropertyAuthorizationRepository(
                getGraph(),
                getOntologyRepository(),
                getConfiguration(),
                getUserNotificationRepository(),
                getWorkQueueRepository(),
                getGraphAuthorizationRepository()
        ) {
            @Override
            protected UserRepository getUserRepository() {
                return OpenLumifyInMemoryTestBase.this.getUserRepository();
            }
        };
        return authorizationRepository;
    }

    protected UserNotificationRepository getUserNotificationRepository() {
        if (userNotificationRepository != null) {
            return userNotificationRepository;
        }
        userNotificationRepository = new InMemoryUserNotificationRepository(
                getGraph(),
                getGraphRepository(),
                getGraphAuthorizationRepository(),
                getWorkQueueRepository()
        ) {
            @Override
            protected UserRepository getUserRepository() {
                return OpenLumifyInMemoryTestBase.this.getUserRepository();
            }

            @Override
            protected AuthorizationRepository getAuthorizationRepository() {
                return OpenLumifyInMemoryTestBase.this.getAuthorizationRepository();
            }
        };
        return userNotificationRepository;
    }

    protected SystemNotificationRepository getSystemNotificationRepository() {
        if (systemNotificationRepository != null) {
            return systemNotificationRepository;
        }
        systemNotificationRepository = new InMemorySystemNotificationRepository(
                getGraph(),
                getGraphRepository(),
                getGraphAuthorizationRepository(),
                getUserRepository()
        ) {
            @Override
            protected AuthorizationRepository getAuthorizationRepository() {
                return OpenLumifyInMemoryTestBase.this.getAuthorizationRepository();
            }
        };
        return systemNotificationRepository;
    }

    protected WorkQueueRepository getWorkQueueRepository() {
        if (workQueueRepository != null) {
            return workQueueRepository;
        }
        WorkQueueNames workQueueNames = new WorkQueueNames(getConfiguration());
        workQueueRepository = new TestWorkQueueRepository(
                getGraph(),
                workQueueNames,
                getConfiguration()
        ) {
            @Override
            protected UserRepository getUserRepository() {
                return OpenLumifyInMemoryTestBase.this.getUserRepository();
            }

            @Override
            protected WorkspaceRepository getWorkspaceRepository() { return OpenLumifyInMemoryTestBase.this.getWorkspaceRepository(); }
        };
        return workQueueRepository;
    }

    protected List<byte[]> getWorkQueueItems(String queueName) {
        WorkQueueRepository workQueueRepository = getWorkQueueRepository();
        if (!(workQueueRepository instanceof TestWorkQueueRepository)) {
            throw new OpenLumifyException("Can only get work queue items from " + TestWorkQueueRepository.class.getName());
        }
        List<byte[]> items = ((TestWorkQueueRepository) workQueueRepository).getWorkQueue(queueName);
        if (items == null) {
            return new ArrayList<>();
        }
        return items;
    }

    protected void clearWorkQueues() {
        WorkQueueRepository workQueueRepository = getWorkQueueRepository();
        if (!(workQueueRepository instanceof TestWorkQueueRepository)) {
            throw new OpenLumifyException("Can only clear work queue items from " + TestWorkQueueRepository.class.getName());
        }
        ((TestWorkQueueRepository) workQueueRepository).clearQueue();
    }

    protected List<JSONObject> getBroadcastJsonValues() {
        WorkQueueRepository workQueueRepository = getWorkQueueRepository();
        if (!(workQueueRepository instanceof TestWorkQueueRepository)) {
            throw new OpenLumifyException("Can only clear work queue items from " + TestWorkQueueRepository.class.getName());
        }
        return ((TestWorkQueueRepository) workQueueRepository).getBroadcastJsonValues();
    }

    protected OntologyRepository getOntologyRepository() {
        if (ontologyRepository != null) {
            return ontologyRepository;
        }
        try {
            ontologyRepository = new InMemoryOntologyRepository(
                    getGraph(),
                    getConfiguration(),
                    getLockRepository(),
                    getCacheService()
            ) {
                @Override
                protected PrivilegeRepository getPrivilegeRepository() {
                    return OpenLumifyInMemoryTestBase.this.getPrivilegeRepository();
                }

                @Override
                protected WorkspaceRepository getWorkspaceRepository() {
                    return OpenLumifyInMemoryTestBase.this.getWorkspaceRepository();
                }
            };
        } catch (Exception ex) {
            throw new OpenLumifyException("Could not create ontology repository", ex);
        }
        return ontologyRepository;
    }

    protected VisibilityTranslator getVisibilityTranslator() {
        if (visibilityTranslator != null) {
            return visibilityTranslator;
        }
        visibilityTranslator = new DirectVisibilityTranslator();
        return visibilityTranslator;
    }

    protected LockRepository getLockRepository() {
        if (lockRepository != null) {
            return lockRepository;
        }
        lockRepository = new NonLockingLockRepository();
        return lockRepository;
    }

    protected GraphAuthorizationRepository getGraphAuthorizationRepository() {
        if (graphAuthorizationRepository != null) {
            return graphAuthorizationRepository;
        }
        graphAuthorizationRepository = new InMemoryGraphAuthorizationRepository();
        return graphAuthorizationRepository;
    }

    protected UserRepository getUserRepository() {
        if (userRepository != null) {
            return userRepository;
        }
        userRepository = new VertexiumUserRepository(
                getConfiguration(),
                getGraphAuthorizationRepository(),
                getGraph(),
                getOntologyRepository(),
                getUserSessionCounterRepository(),
                getLockRepository(),
                getAuthorizationRepository(),
                getPrivilegeRepository()
        ) {
            @Override
            protected Collection<UserListener> getUserListeners() {
                return OpenLumifyInMemoryTestBase.this.getUserListeners();
            }
        };
        getGraph().defineProperty(UserOpenLumifyProperties.USERNAME.getPropertyName()).dataType(String.class).textIndexHint(TextIndexHint.EXACT_MATCH).define();
        getGraph().defineProperty(UserOpenLumifyProperties.DISPLAY_NAME.getPropertyName()).dataType(String.class).textIndexHint(TextIndexHint.EXACT_MATCH).define();
        getGraph().defineProperty(UserOpenLumifyProperties.CREATE_DATE.getPropertyName()).dataType(String.class).define();
        getGraph().defineProperty(UserOpenLumifyProperties.PASSWORD_HASH.getPropertyName()).dataType(String.class).define();
        getGraph().defineProperty(UserOpenLumifyProperties.PASSWORD_SALT.getPropertyName()).dataType(String.class).define();
        getGraph().defineProperty(UserOpenLumifyProperties.EMAIL_ADDRESS.getPropertyName()).dataType(String.class).define();
        return userRepository;
    }

    protected Collection<UserListener> getUserListeners() {
        return new ArrayList<>();
    }

    protected void setPrivileges(User user, Set<String> privileges) {
        ((UserPropertyPrivilegeRepository) getPrivilegeRepository()).setPrivileges(user, privileges, getUserRepository().getSystemUser());
    }

    protected PrivilegeRepository getPrivilegeRepository() {
        if (privilegeRepository != null) {
            return privilegeRepository;
        }
        privilegeRepository = new UserPropertyPrivilegeRepository(
                getOntologyRepository(),
                getConfiguration(),
                getUserNotificationRepository(),
                getWorkQueueRepository()
        ) {
            @Override
            protected Iterable<PrivilegesProvider> getPrivilegesProviders(Configuration configuration) {
                return OpenLumifyInMemoryTestBase.this.getPrivilegesProviders();
            }

            @Override
            protected UserRepository getUserRepository() {
                return OpenLumifyInMemoryTestBase.this.getUserRepository();
            }
        };
        return privilegeRepository;
    }

    protected Iterable<PrivilegesProvider> getPrivilegesProviders() {
        return new ArrayList<>();
    }

    protected UserSessionCounterRepository getUserSessionCounterRepository() {
        if (userSessionCounterRepository != null) {
            return userSessionCounterRepository;
        }
        userSessionCounterRepository = new InMemoryUserSessionCounterRepository(
                getTimeRepository()
        );
        return userSessionCounterRepository;
    }

    protected TimeRepository getTimeRepository() {
        if (timeRepository != null) {
            return timeRepository;
        }
        timeRepository = new MockTimeRepository();
        return timeRepository;
    }

    protected FileSystemRepository getFileSystemRepository() {
        if (fileSystemRepository != null) {
            return fileSystemRepository;
        }
        fileSystemRepository = new ClassPathFileSystemRepository("");
        return fileSystemRepository;
    }

    protected Configuration getConfiguration() {
        if (configuration != null) {
            return configuration;
        }
        Map config = getConfigurationMap();
        HashMapConfigurationLoader configLoader = new HashMapConfigurationLoader(config);

        configuration = new Configuration(configLoader, config) {
            @Override
            protected OntologyRepository getOntologyRepository() {
                return OpenLumifyInMemoryTestBase.this.getOntologyRepository();
            }

            @Override
            protected PrivilegeRepository getPrivilegeRepository() {
                return OpenLumifyInMemoryTestBase.this.getPrivilegeRepository();
            }
        };
        return configuration;
    }

    @SuppressWarnings("unchecked")
    protected Map getConfigurationMap() {
        if (configurationMap != null) {
            return configurationMap;
        }
        configurationMap = new HashMap();
        configurationMap.put("org.openlumify.core.model.user.UserPropertyAuthorizationRepository.defaultAuthorizations", "");
        configurationMap.put("org.openlumify.core.model.user.UserPropertyPrivilegeRepository.defaultPrivileges", "");
        return configurationMap;
    }

    protected GraphRepository getGraphRepository() {
        if (graphRepository != null) {
            return graphRepository;
        }
        graphRepository = new GraphRepository(
                getGraph(),
                getVisibilityTranslator(),
                getTermMentionRepository(),
                getWorkQueueRepository()
        );
        return graphRepository;
    }

    protected Graph getGraph() {
        if (graph != null) {
            return graph;
        }
        graph = InMemoryGraph.create();
        return graph;
    }

    protected WorkProductService getWorkProductServiceByKind(String kind) {
        if (kind.equals(GRAPH_WORK_PRODUCT_KIND)) {
            return getGraphWorkProduct();
        }
        if (kind.equals(MAP_WORK_PRODUCT_KIND)) {
            return getMapWorkProduct();
        }
        throw new OpenLumifyException("unhandled getWorkProductServiceByKind: " + kind);
    }

    protected WorkProductService getGraphWorkProduct() {
        if (graphWorkProduct != null) {
            return graphWorkProduct;
        }
        graphWorkProduct = new WorkProductService() {
            @Override
            public WorkProductExtendedData getExtendedData(
                    Graph graph,
                    Vertex workspaceVertex,
                    Vertex productVertex,
                    GetExtendedDataParams params,
                    User user,
                    Authorizations authorizations
            ) {
                return getWorkProductExtendedData(
                        GRAPH_WORK_PRODUCT_KIND,
                        graph,
                        workspaceVertex,
                        productVertex,
                        params,
                        user,
                        authorizations
                );
            }

            @Override
            public String getKind() {
                return GRAPH_WORK_PRODUCT_KIND;
            }
        };
        return graphWorkProduct;
    }

    protected WorkProductService getMapWorkProduct() {
        if (mapWorkProduct != null) {
            return mapWorkProduct;
        }
        mapWorkProduct = new WorkProductService() {
            @Override
            public WorkProductExtendedData getExtendedData(
                    Graph graph,
                    Vertex workspaceVertex,
                    Vertex productVertex,
                    GetExtendedDataParams params,
                    User user,
                    Authorizations authorizations
            ) {
                return getWorkProductExtendedData(
                        MAP_WORK_PRODUCT_KIND,
                        graph,
                        workspaceVertex,
                        productVertex,
                        params,
                        user,
                        authorizations
                );
            }

            @Override
            public String getKind() {
                return MAP_WORK_PRODUCT_KIND;
            }
        };
        return mapWorkProduct;
    }

    protected WorkProductExtendedData getWorkProductExtendedData(
            String workProductKind,
            Graph graph,
            Vertex workspaceVertex,
            Vertex productVertex,
            GetExtendedDataParams params,
            User user,
            Authorizations authorizations
    ) {
        WorkProductExtendedData data = new WorkProductExtendedData();
        data.setEdges(new HashMap<>());
        data.setVertices(new HashMap<>());
        return data;
    }

    public LongRunningProcessRepository getLongRunningProcessRepository() {
        if (longRunningProcessRepository != null) {
            return longRunningProcessRepository;
        }
        longRunningProcessRepository = new VertexiumLongRunningProcessRepository(
                getGraphRepository(),
                getGraphAuthorizationRepository(),
                getUserRepository(),
                getWorkQueueRepository(),
                getGraph(),
                getAuthorizationRepository()
        );
        return longRunningProcessRepository;
    }

    public WorkQueueNames getWorkQueueNames() {
        if (workQueueNames != null) {
            return workQueueNames;
        }
        workQueueNames = new WorkQueueNames(getConfiguration());
        return workQueueNames;
    }

    public WorkspaceHelper getWorkspaceHelper() {
        if (workspaceHelper != null) {
            return workspaceHelper;
        }
        workspaceHelper = new WorkspaceHelper(
                getTermMentionRepository(),
                getWorkQueueRepository(),
                getGraph(),
                getOntologyRepository(),
                getWorkspaceRepository(),
                getPrivilegeRepository(),
                getAuthorizationRepository()
        );
        return workspaceHelper;
    }

    public CacheService getCacheService() {
        if (cacheService != null) {
            return cacheService;
        }
        cacheService = new NopCacheService();
        return cacheService;
    }

    public ThumbnailRepository getThumbnailRepository() {
        if (thumbnailRepository != null) {
            return thumbnailRepository;
        }
        thumbnailRepository = new InMemoryThumbnailRepository(
                getOntologyRepository()
        );
        return thumbnailRepository;
    }

    public AuditService getAuditService() {
        if (auditService != null) {
            return auditService;
        }
        auditService = new LoggingAuditService();
        return auditService;
    }

    public ActionRepository getActionRepository() {
        if (actionRepository != null) {
            return actionRepository;
        }
        actionRepository = new ActionRepository(getConfiguration()) {
            @Override
            protected Collection<Action> getActions() {
                return getActionRepositoryActions();
            }

            @Override
            public Action getActionFromActionData(JSONObject json) {
                return getActionRepositoryActionFromActionData(json);
            }
        };
        return actionRepository;
    }

    protected Action getActionRepositoryActionFromActionData(JSONObject json) {
        return getActionRepository().getActionFromActionData(json);
    }

    protected Collection<Action> getActionRepositoryActions() {
        return new ArrayList<>();
    }

    public Authorizations getGraphAuthorizations(String... authorizations) {
        return getGraph().createAuthorizations(authorizations);
    }

    public Authorizations getGraphAuthorizations(User user, String... authorizations) {
        return getAuthorizationRepository().getGraphAuthorizations(user, authorizations);
    }

    protected User getUser() {
        if (user == null) {
            user = new InMemoryUser("test", "Test User", "test@openlumify.org", null);
        }
        return user;
    }

    protected List<byte[]> getGraphPropertyQueueItems() {
        return getWorkQueueItems(getWorkQueueNames().getGraphPropertyQueueName());
    }
}
