package org.openlumify.web.structuredingest.core.routes;

import com.google.inject.Singleton;
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
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.model.longRunningProcess.LongRunningProcessRepository;
import org.openlumify.core.model.ontology.OntologyRepository;
import org.openlumify.core.model.properties.OpenLumifyProperties;
import org.openlumify.core.model.user.PrivilegeRepository;
import org.openlumify.core.model.workQueue.WorkQueueRepository;
import org.openlumify.core.model.workspace.WorkspaceHelper;
import org.openlumify.core.model.workspace.WorkspaceRepository;
import org.openlumify.core.security.VisibilityTranslator;
import org.openlumify.core.user.User;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;
import org.openlumify.web.OpenLumifyResponse;
import org.openlumify.web.clientapi.model.ClientApiObject;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;
import org.openlumify.web.structuredingest.core.model.ClientApiMappingErrors;
import org.openlumify.web.structuredingest.core.model.StructuredIngestParser;
import org.openlumify.web.structuredingest.core.util.StructuredIngestParserFactory;
import org.openlumify.web.structuredingest.core.model.StructuredIngestQueueItem;
import org.openlumify.web.structuredingest.core.util.BaseStructuredFileParserHandler;
import org.openlumify.web.structuredingest.core.util.GraphBuilderParserHandler;
import org.openlumify.web.structuredingest.core.model.ParseOptions;
import org.openlumify.web.structuredingest.core.util.ProgressReporter;
import org.openlumify.web.structuredingest.core.util.mapping.ParseMapping;
import org.openlumify.web.structuredingest.core.worker.StructuredIngestProcessWorker;

import javax.inject.Inject;
import java.io.InputStream;

@Singleton
public class Ingest implements ParameterizedHandler {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(Ingest.class);

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
            throw new OpenLumifyResourceNotFoundException("Could not find vertex:" + graphVertexId);
        }

        StreamingPropertyValue rawPropertyValue = OpenLumifyProperties.RAW.getPropertyValue(vertex);
        if (rawPropertyValue == null) {
            throw new OpenLumifyResourceNotFoundException("Could not find raw property on vertex:" + graphVertexId);
        }

        ParseMapping parseMapping = new ParseMapping(ontologyRepository, visibilityTranslator, workspaceId, mapping);
        ClientApiMappingErrors mappingErrors = parseMapping.validate(authorizations);
        if (mappingErrors.mappingErrors.size() > 0) {
            return mappingErrors;
        }


        if (preview) {
            return previewIngest(user, workspaceId, authorizations, optionsJson, publish, vertex, rawPropertyValue, parseMapping);
        } else {
            return enqueueIngest(user, workspaceId, authorizations, graphVertexId, mapping, optionsJson, publish);
        }
    }

    private ClientApiObject enqueueIngest(User user, String workspaceId, Authorizations authorizations, String graphVertexId, String mapping, String optionsJson, boolean publish) {
        StructuredIngestQueueItem queueItem = new StructuredIngestQueueItem(workspaceId, mapping, user.getUserId(), graphVertexId, StructuredIngestProcessWorker.TYPE, new ParseOptions(optionsJson), publish, authorizations);
        this.longRunningProcessRepository.enqueue(queueItem.toJson(), user, authorizations);
        return OpenLumifyResponse.SUCCESS;
    }

    private ClientApiObject previewIngest(User user, String workspaceId, Authorizations authorizations, String optionsJson, boolean publish, Vertex vertex, StreamingPropertyValue rawPropertyValue, ParseMapping parseMapping) throws Exception {
        JSONObject data = new JSONObject();
        JSONObject permissions = new JSONObject();
        JSONArray users = new JSONArray();
        users.put(user.getUserId());
        permissions.put("users", users);

        ProgressReporter reporter = new ProgressReporter() {
            public void finishedRow(long row, long totalRows) {
                if (totalRows != -1) {
                    long total = Math.min(GraphBuilderParserHandler.MAX_DRY_RUN_ROWS, totalRows);
                    data.put("row", row);
                    data.put("total", total);

                    // Broadcast when we get this change in percent
                    int percent = (int) ((double)total * 0.01);

                    if (percent > 0 && row % percent == 0) {
                        workQueueRepository.broadcast("structuredImportDryrun", data, permissions);
                    }
                }
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

        parse(vertex, rawPropertyValue, parseOptions, parserHandler);

        if (parserHandler.hasErrors()) {
            return parserHandler.parseErrors;
        }
        return parserHandler.clientApiIngestPreview;
    }

    private void parse(Vertex vertex, StreamingPropertyValue rawPropertyValue, ParseOptions parseOptions, BaseStructuredFileParserHandler parserHandler) throws Exception {
        String mimeType = (String) vertex.getPropertyValue(OpenLumifyProperties.MIME_TYPE.getPropertyName());
        if (mimeType == null) {
            throw new OpenLumifyException("No mimeType property found for vertex");
        }

        StructuredIngestParser structuredIngestParser = structuredIngestParserFactory.getParser(mimeType);
        if (structuredIngestParser == null) {
            throw new OpenLumifyException("No parser registered for mimeType: " + mimeType);
        }

        try (InputStream in = rawPropertyValue.getInputStream()) {
            structuredIngestParser.ingest(in, parseOptions, parserHandler);
        }
    }
}