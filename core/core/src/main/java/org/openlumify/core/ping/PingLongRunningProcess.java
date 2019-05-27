package org.openlumify.core.ping;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.json.JSONObject;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.openlumify.core.model.Description;
import org.openlumify.core.model.Name;
import org.openlumify.core.model.longRunningProcess.LongRunningProcessWorker;
import org.openlumify.core.model.user.AuthorizationRepository;
import org.openlumify.core.model.user.UserRepository;
import org.openlumify.core.util.ClientApiConverter;

@Name("Ping")
@Description("run on special Ping vertices to measure LRP wait time")
@Singleton
public class PingLongRunningProcess extends LongRunningProcessWorker {
    private final UserRepository userRepository;
    private final Graph graph;
    private final PingUtil pingUtil;
    private final AuthorizationRepository authorizationRepository;

    @Inject
    public PingLongRunningProcess(
            AuthorizationRepository authorizationRepository,
            UserRepository userRepository,
            Graph graph,
            PingUtil pingUtil
    ) {
        this.authorizationRepository = authorizationRepository;
        this.userRepository = userRepository;
        this.graph = graph;
        this.pingUtil = pingUtil;
    }

    @Override
    protected void processInternal(JSONObject jsonObject) {
        PingLongRunningProcessQueueItem queueItem = ClientApiConverter.toClientApi(
                jsonObject.toString(),
                PingLongRunningProcessQueueItem.class
        );
        Authorizations authorizations = authorizationRepository.getGraphAuthorizations(userRepository.getSystemUser());
        Vertex vertex = graph.getVertex(queueItem.getVertexId(), authorizations);
        pingUtil.lrpUpdate(vertex, graph, authorizations);
    }

    @Override
    public boolean isHandled(JSONObject jsonObject) {
        return PingLongRunningProcessQueueItem.isHandled(jsonObject);
    }
}
