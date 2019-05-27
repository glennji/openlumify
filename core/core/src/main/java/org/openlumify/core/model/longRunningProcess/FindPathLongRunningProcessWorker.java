package org.openlumify.core.model.longRunningProcess;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.json.JSONObject;
import org.vertexium.*;
import org.openlumify.core.model.Description;
import org.openlumify.core.model.Name;
import org.openlumify.core.util.ClientApiConverter;
import org.openlumify.web.clientapi.model.ClientApiVertexFindPathResponse;

import java.util.ArrayList;
import java.util.List;

@Name("Find Path")
@Description("Finds a path between two vertices")
@Singleton
public class FindPathLongRunningProcessWorker extends LongRunningProcessWorker {
    private final Graph graph;
    private final LongRunningProcessRepository longRunningProcessRepository;

    @Inject
    public FindPathLongRunningProcessWorker(
            Graph graph,
            LongRunningProcessRepository longRunningProcessRepository
    ) {
        this.graph = graph;
        this.longRunningProcessRepository = longRunningProcessRepository;
    }

    @Override
    public boolean isHandled(JSONObject longRunningProcessQueueItem) {
        return longRunningProcessQueueItem.getString("type").equals("findPath");
    }

    @Override
    public void processInternal(final JSONObject longRunningProcessQueueItem) {
        FindPathLongRunningProcessQueueItem findPath = ClientApiConverter.toClientApi(longRunningProcessQueueItem.toString(), FindPathLongRunningProcessQueueItem.class);

        Authorizations authorizations = getAuthorizations(findPath.getAuthorizations());
        String[] labels = findPath.getLabels();
        int hops = findPath.getHops();

        ClientApiVertexFindPathResponse results = new ClientApiVertexFindPathResponse();
        ProgressCallback progressCallback = new ProgressCallback() {
            @Override
            public void progress(double progressPercent, Step step, Integer edgeIndex, Integer vertexCount) {
                longRunningProcessRepository.reportProgress(longRunningProcessQueueItem, progressPercent, step.formatMessage(edgeIndex, vertexCount));
            }
        };

        FindPathOptions findPathOptions = new FindPathOptions(findPath.getOutVertexId(), findPath.getInVertexId(), hops)
                .setLabels(labels)
                .setProgressCallback(progressCallback);
        Iterable<Path> paths = graph.findPaths(findPathOptions, authorizations);
        for (Path path : paths) {
            List<String> clientApiVertexPath = new ArrayList<>();
            for (String s : path) {
                clientApiVertexPath.add(s);
            }
            results.getPaths().add(clientApiVertexPath);
        }

        String resultsString = ClientApiConverter.clientApiToString(results);
        JSONObject resultsJson = new JSONObject(resultsString);
        longRunningProcessQueueItem.put("results", resultsJson);
        longRunningProcessQueueItem.put("resultsCount", results.getPaths().size());
    }

    private Authorizations getAuthorizations(String[] authorizations) {
        return graph.createAuthorizations(authorizations);
    }
}
