package org.visallo.web.structuredingest.core.routes;

import com.google.common.util.concurrent.RateLimiter;
import com.google.inject.Singleton;
import org.visallo.web.structuredingest.core.model.*;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Optional;
import org.visallo.webster.annotations.Required;
import org.json.JSONArray;
import org.json.JSONObject;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.vertexium.property.StreamingPropertyValue;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.longRunningProcess.LongRunningProcessRepository;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.user.PrivilegeRepository;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.WorkspaceHelper;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiObject;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;
import org.visallo.web.structuredingest.core.util.StructuredIngestParserFactory;
import org.visallo.web.structuredingest.core.util.BaseStructuredFileParserHandler;
import org.visallo.web.structuredingest.core.util.GraphBuilderParserHandler;
import org.visallo.web.structuredingest.core.util.ProgressReporter;
import org.visallo.web.structuredingest.core.util.mapping.ParseMapping;
import org.visallo.web.structuredingest.core.worker.StructuredIngestProcessWorker;

import javax.inject.Inject;
import java.io.InputStream;

@Singleton
public class Ingest implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(Ingest.class);

    private final LongRunningProcessRepository longRunningProcessRepository;
    private final OntologyRepository ontologyRepository;
    private final PrivilegeRepository privilegeRepository;
    private final VisibilityTranslator visibilityTranslator;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceHelper workspaceHelper;
    private final WorkQueueRepository workQueueRepository;
    private final Graph graph;
    private final StructuredIngestParserFactory structuredIngestParserFactory;

    @Inject
    public Ingest(
        LongRunningProcessRepository longRunningProcessRepository,
        OntologyRepository ontologyRepository,
        PrivilegeRepository privilegeRepository,
        WorkspaceRepository workspaceRepository,
        WorkspaceHelper workspaceHelper,
        StructuredIngestParserFactory structuredIngestParserFactory,
        WorkQueueRepository workQueueRepository,
        VisibilityTranslator visibilityTranslator,
        Graph graph
    ) {
        this.longRunningProcessRepository = longRunningProcessRepository;
        this.ontologyRepository = ontologyRepository;
        this.privilegeRepository = privilegeRepository;
        this.workspaceHelper = workspaceHelper;
        this.workspaceRepository = workspaceRepository;
        this.visibilityTranslator = visibilityTranslator;
        this.structuredIngestParserFactory = structuredIngestParserFactory;
        this.workQueueRepository = workQueueRepository;
        this.graph = graph;
    }

    @Handle
    public ClientApiObject handle(
            User user,
            @ActiveWorkspaceId String workspaceId,
            Authorizations authorizations,
            @Required(name = "graphVertexId") String graphVertexId,
            @Required(name = "mapping") String mapping,
            @Optional(name = "parseOptions") String optionsJson,
            @Optional(name = "publish", defaultValue = "false") boolean publish,
            @Optional(name = "preview", defaultValue = "true") boolean preview
    ) throws Exception {

        Vertex vertex = graph.getVertex(graphVertexId, authorizations);
        if (vertex == null) {
            throw new VisalloResourceNotFoundException("Could not find vertex:" + graphVertexId);
        }

        ParseMapping parseMapping = new ParseMapping(ontologyRepository, visibilityTranslator, workspaceId, mapping);
        ClientApiMappingErrors mappingErrors = parseMapping.validate(authorizations);
        if (mappingErrors.mappingErrors.size() > 0) {
            return mappingErrors;
        }


        if (preview) {
            return previewIngest(user, workspaceId, authorizations, optionsJson, publish, vertex, parseMapping);
        } else {
            return enqueueIngest(user, workspaceId, authorizations, graphVertexId, mapping, optionsJson, publish);
        }
    }

    private ClientApiObject enqueueIngest(User user, String workspaceId, Authorizations authorizations, String graphVertexId, String mapping, String optionsJson, boolean publish) {
        StructuredIngestQueueItem queueItem = new StructuredIngestQueueItem(workspaceId, mapping, user.getUserId(), graphVertexId, StructuredIngestProcessWorker.TYPE, new ParseOptions(optionsJson), publish, authorizations);
        this.longRunningProcessRepository.enqueue(queueItem.toJson(), user, authorizations);
        return VisalloResponse.SUCCESS;
    }

    private ClientApiObject previewIngest(User user, String workspaceId, Authorizations authorizations, String optionsJson, boolean publish, Vertex vertex, ParseMapping parseMapping) throws Exception {
        JSONObject data = new JSONObject();
        JSONObject permissions = new JSONObject();
        JSONArray users = new JSONArray();
        users.put(user.getUserId());
        permissions.put("users", users);

        ProgressReporter reporter = new ProgressReporter(new double[] { 0.1, 0.9 }) {
            public void reportThrottled(String msg, long current, long totalRows, double totalPercent, String remaining) {
                long total = Math.min(GraphBuilderParserHandler.MAX_DRY_RUN_ROWS, totalRows);
                data.put("current", current);
                data.put("total", total);
                data.put("totalPercent", totalPercent);
                data.put("message", msg);
                data.putOpt("remaining", remaining);
                workQueueRepository.broadcast("structuredImportDryrun", data, permissions);
            }
        };

        GraphBuilderParserHandler parserHandler = new GraphBuilderParserHandler(
                graph,
                user,
                visibilityTranslator,
                privilegeRepository,
                authorizations,
                workspaceRepository,
                workspaceHelper,
                workspaceId,
                publish,
                vertex,
                parseMapping,
                reporter);

        parserHandler.dryRun = true;
        ParseOptions parseOptions = new ParseOptions(optionsJson);

        parse(vertex, parseOptions, parserHandler);

        if (parserHandler.hasErrors()) {
            return parserHandler.parseErrors;
        }
        return parserHandler.clientApiIngestPreview;
    }

    private void parse(Vertex vertex, ParseOptions parseOptions, BaseStructuredFileParserHandler parserHandler) throws Exception {
        String mimeType = (String) vertex.getPropertyValue(VisalloProperties.MIME_TYPE.getPropertyName());
        if (mimeType == null) {
            throw new VisalloException("No mimeType property found for vertex");
        }

        StructuredIngestParser structuredIngestParser = structuredIngestParserFactory.getParser(mimeType);
        if (structuredIngestParser == null) {
            throw new VisalloException("No parser registered for mimeType: " + mimeType);
        }

        structuredIngestParser.ingest(new VertexRawStructuredImportSource(vertex), parseOptions, parserHandler);
    }
}