package org.openlumify.core.model.longRunningProcess;

import com.google.inject.Injector;
import org.json.JSONObject;
import org.mockito.Mock;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.inmemory.InMemoryGraph;
import org.openlumify.core.model.graph.GraphRepository;
import org.openlumify.core.model.termMention.TermMentionRepository;
import org.openlumify.core.model.user.AuthorizationRepository;
import org.openlumify.core.model.user.UserRepository;
import org.openlumify.core.model.workQueue.WorkQueueRepository;
import org.openlumify.core.security.VisibilityTranslator;
import org.openlumify.core.status.MetricsManager;
import org.openlumify.core.user.SystemUser;
import org.openlumify.core.user.User;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class LongRunningProcessWorkerTestBase {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(LongRunningProcessWorkerTestBase.class);
    private Graph graph;
    private GraphRepository graphRepository;

    private SystemUser systemUser = new SystemUser();

    @Mock
    private User user;
    @Mock
    private LongRunningProcessRepository longRunningProcessRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthorizationRepository authorizationRepository;
    @Mock
    private Injector injector;
    @Mock
    private MetricsManager metricsManager;
    @Mock
    private com.codahale.metrics.Counter mockCounter;
    @Mock
    private com.codahale.metrics.Timer mockTimer;
    @Mock
    private com.codahale.metrics.Meter mockMeter;
    @Mock
    private VisibilityTranslator visibilityTranslator;
    @Mock
    private TermMentionRepository termMentionRepository;
    @Mock
    private WorkQueueRepository workQueueRepository;
    @Mock
    private Authorizations systemUserAuthorizations;

    protected void before() {
        graph = InMemoryGraph.create();
        when(metricsManager.counter(any())).thenReturn(mockCounter);
        when(metricsManager.counter(any(), any())).thenReturn(mockCounter);
        when(metricsManager.timer(any())).thenReturn(mockTimer);
        when(metricsManager.timer(any(), any())).thenReturn(mockTimer);
        when(metricsManager.meter(any())).thenReturn(mockMeter);
        when(metricsManager.meter(any(), any())).thenReturn(mockMeter);
        when(userRepository.getSystemUser()).thenReturn(systemUser);
        when(authorizationRepository.getGraphAuthorizations(systemUser)).thenReturn(systemUserAuthorizations);
    }

    protected void prepare(LongRunningProcessWorker worker) {
        worker.prepare(getLongRunningWorkerPrepareData());
    }

    protected LongRunningWorkerPrepareData getLongRunningWorkerPrepareData() {
        return new LongRunningWorkerPrepareData(
                getConfig(),
                getUser(),
                getInjector()
        );
    }

    private Injector getInjector() {
        return injector;
    }

    protected Graph getGraph() {
        return graph;
    }

    protected LongRunningProcessRepository getLongRunningProcessRepository() {
        return longRunningProcessRepository;
    }

    protected UserRepository getUserRepository() {
        return userRepository;
    }

    protected AuthorizationRepository getAuthorizationRepository() {
        return authorizationRepository;
    }

    protected User getUser() {
        return user;
    }

    protected Map getConfig() {
        return new HashMap();
    }

    protected MetricsManager getMetricsManager() {
        return metricsManager;
    }

    public GraphRepository getGraphRepository() {
        if (graphRepository == null) {
            graphRepository = new GraphRepository(
                    getGraph(),
                    getVisibilityTranslator(),
                    getTermMentionRepository(),
                    getWorkQueueRepository()
            );
        }
        return graphRepository;
    }

    protected void run(LongRunningProcessWorker worker, JSONObject queueItem) {
        if (worker.isHandled(queueItem)) {
            worker.process(queueItem);
        } else {
            LOGGER.warn("Unhandled: %s", queueItem.toString());
        }
    }

    public VisibilityTranslator getVisibilityTranslator() {
        return visibilityTranslator;
    }

    public TermMentionRepository getTermMentionRepository() {
        return termMentionRepository;
    }

    public WorkQueueRepository getWorkQueueRepository() {
        return workQueueRepository;
    }
}
