package org.openlumify.core.ping;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.json.JSONObject;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.vertexium.Visibility;
import org.vertexium.mutation.ElementMutation;
import org.vertexium.query.*;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.model.longRunningProcess.LongRunningProcessRepository;
import org.openlumify.core.model.properties.OpenLumifyProperties;
import org.openlumify.core.model.user.GraphAuthorizationRepository;
import org.openlumify.core.model.user.UserRepository;
import org.openlumify.core.model.workQueue.Priority;
import org.openlumify.core.model.workQueue.WorkQueueRepository;
import org.openlumify.core.security.OpenLumifyVisibility;
import org.openlumify.core.security.VisibilityTranslator;
import org.openlumify.core.user.User;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;

@Singleton
public class PingUtil {
    public static final String VISIBILITY_STRING = "ping";
    public static final Visibility VISIBILITY = new OpenLumifyVisibility(VISIBILITY_STRING).getVisibility();
    private final User systemUser;
    private final VisibilityTranslator visibilityTranslator;

    @Inject
    public PingUtil(
            GraphAuthorizationRepository graphAuthorizationRepository,
            UserRepository userRepository,
            VisibilityTranslator visibilityTranslator
    ) {
        graphAuthorizationRepository.addAuthorizationToGraph(VISIBILITY_STRING);
        this.systemUser = userRepository.getSystemUser();
        this.visibilityTranslator = visibilityTranslator;
    }

    public String search(Graph graph, Authorizations authorizations) {
        Query query = graph.query(authorizations).limit(1);
        List<Vertex> vertices = Lists.newArrayList(query.vertices());
        if (vertices.size() == 0) {
            throw new OpenLumifyException("query returned no vertices");
        } else if (vertices.size() > 1) {
            throw new OpenLumifyException("query returned more than one vertex");
        }
        return vertices.get(0).getId();
    }

    public void retrieve(String vertexId, Graph graph, Authorizations authorizations) {
        Vertex retrievedVertex = graph.getVertex(vertexId, authorizations);
        if (retrievedVertex == null) {
            throw new OpenLumifyException("failed to retrieve vertex by id: " + vertexId);
        }
    }

    public Vertex createVertex(String remoteAddr, long searchTime, long retrievalTime, Graph graph, Authorizations authorizations) {
        Date createDate = new Date();
        String vertexId = PingOntology.getVertexId(createDate);
        ElementMutation<Vertex> mutation = graph.prepareVertex(vertexId, VISIBILITY);
        OpenLumifyProperties.CONCEPT_TYPE.setProperty(mutation, PingOntology.IRI_CONCEPT_PING, visibilityTranslator.getDefaultVisibility());
        PingOntology.CREATE_DATE.setProperty(mutation, createDate, VISIBILITY);
        PingOntology.CREATE_REMOTE_ADDR.setProperty(mutation, remoteAddr, VISIBILITY);
        PingOntology.SEARCH_TIME_MS.setProperty(mutation, searchTime, VISIBILITY);
        PingOntology.RETRIEVAL_TIME_MS.setProperty(mutation, retrievalTime, VISIBILITY);
        Vertex vertex = mutation.save(authorizations);
        graph.flush();
        return vertex;
    }

    public void enqueueToWorkQueue(Vertex vertex, WorkQueueRepository workQueueRepository, Priority priority) {
        workQueueRepository.pushElement(vertex, priority);
    }

    public void gpwUpdate(Vertex vertex, Graph graph, Authorizations authorizations) {
        Date updateDate = new Date();
        Long waitTimeMs = updateDate.getTime() - PingOntology.CREATE_DATE.getPropertyValueRequired(vertex).getTime();
        ElementMutation<Vertex> mutation = vertex.prepareMutation();
        PingOntology.GRAPH_PROPERTY_WORKER_DATE.setProperty(mutation, updateDate, VISIBILITY);
        PingOntology.GRAPH_PROPERTY_WORKER_HOSTNAME.setProperty(mutation, getHostname(), VISIBILITY);
        PingOntology.GRAPH_PROPERTY_WORKER_HOST_ADDRESS.setProperty(mutation, getHostAddress(), VISIBILITY);
        PingOntology.GRAPH_PROPERTY_WORKER_WAIT_TIME_MS.setProperty(mutation, waitTimeMs, VISIBILITY);
        mutation.save(authorizations);
        graph.flush();
    }

    public void enqueueToLongRunningProcess(Vertex vertex, LongRunningProcessRepository longRunningProcessRepository, Authorizations authorizations) {
        longRunningProcessRepository.enqueue(new PingLongRunningProcessQueueItem(vertex).toJson(), systemUser, authorizations);
    }

    public void lrpUpdate(Vertex vertex, Graph graph, Authorizations authorizations) {
        Date updateDate = new Date();
        Long waitTimeMs = updateDate.getTime() - PingOntology.CREATE_DATE.getPropertyValueRequired(vertex).getTime();
        ElementMutation<Vertex> mutation = vertex.prepareMutation();
        PingOntology.LONG_RUNNING_PROCESS_DATE.setProperty(mutation, updateDate, VISIBILITY);
        PingOntology.LONG_RUNNING_PROCESS_HOSTNAME.setProperty(mutation, getHostname(), VISIBILITY);
        PingOntology.LONG_RUNNING_PROCESS_HOST_ADDRESS.setProperty(mutation, getHostAddress(), VISIBILITY);
        PingOntology.LONG_RUNNING_PROCESS_WAIT_TIME_MS.setProperty(mutation, waitTimeMs, VISIBILITY);
        mutation.save(authorizations);
        graph.flush();
    }

    public JSONObject getAverages(int minutes, Graph graph, Authorizations authorizations) {
        Date minutesAgo = new Date(System.currentTimeMillis() - minutes * 60 * 1000);
        Query q = graph.query(authorizations)
                .has(OpenLumifyProperties.CONCEPT_TYPE.getPropertyName(), PingOntology.IRI_CONCEPT_PING)
                .has(PingOntology.CREATE_DATE.getPropertyName(), Compare.GREATER_THAN, minutesAgo)
                .limit(0);
        q.addAggregation(new StatisticsAggregation(PingOntology.SEARCH_TIME_MS.getPropertyName(), PingOntology.SEARCH_TIME_MS.getPropertyName()));
        q.addAggregation(new StatisticsAggregation(PingOntology.RETRIEVAL_TIME_MS.getPropertyName(), PingOntology.RETRIEVAL_TIME_MS.getPropertyName()));
        q.addAggregation(new StatisticsAggregation(PingOntology.GRAPH_PROPERTY_WORKER_WAIT_TIME_MS.getPropertyName(), PingOntology.GRAPH_PROPERTY_WORKER_WAIT_TIME_MS.getPropertyName()));
        q.addAggregation(new StatisticsAggregation(PingOntology.LONG_RUNNING_PROCESS_WAIT_TIME_MS.getPropertyName(), PingOntology.LONG_RUNNING_PROCESS_WAIT_TIME_MS.getPropertyName()));
        QueryResultsIterable<Vertex> vertices = q.vertices();
        StatisticsResult searchTimeAgg = vertices.getAggregationResult(PingOntology.SEARCH_TIME_MS.getPropertyName(), StatisticsResult.class);
        StatisticsResult retrievalTimeAgg = vertices.getAggregationResult(PingOntology.RETRIEVAL_TIME_MS.getPropertyName(), StatisticsResult.class);
        StatisticsResult gpwWaitTimeAgg = vertices.getAggregationResult(PingOntology.GRAPH_PROPERTY_WORKER_WAIT_TIME_MS.getPropertyName(), StatisticsResult.class);
        StatisticsResult lrpWaitTimeAgg = vertices.getAggregationResult(PingOntology.LONG_RUNNING_PROCESS_WAIT_TIME_MS.getPropertyName(), StatisticsResult.class);

        JSONObject json = new JSONObject();
        json.put("pingCount", searchTimeAgg.getCount());
        json.put("averageSearchTime", searchTimeAgg.getAverage());
        json.put("averageRetrievalTime", retrievalTimeAgg.getAverage());
        json.put("graphPropertyWorkerCount", gpwWaitTimeAgg.getCount());
        json.put("averageGraphPropertyWorkerWaitTime", gpwWaitTimeAgg.getAverage());
        json.put("longRunningProcessCount", lrpWaitTimeAgg.getCount());
        json.put("averageLongRunningProcessWaitTime", lrpWaitTimeAgg.getAverage());
        return json;
    }

    private String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            // do nothing
        }
        return "";
    }

    private String getHostAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            // do nothing
        }
        return "";
    }
}
