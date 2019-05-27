package org.openlumify.web.routes.vertex;

import com.google.common.io.Files;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.webster.ParameterizedHandler;
import org.openlumify.webster.annotations.Handle;
import org.openlumify.webster.annotations.Optional;
import org.openlumify.webster.annotations.Required;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.ParameterParser;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.vertexium.Visibility;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.ingest.FileImport;
import org.openlumify.core.ingest.cloud.CloudImportLongRunningProcessQueueItem;
import org.openlumify.core.model.longRunningProcess.FindPathLongRunningProcessQueueItem;
import org.openlumify.core.model.longRunningProcess.LongRunningProcessRepository;
import org.openlumify.core.model.workQueue.Priority;
import org.openlumify.core.model.workspace.Workspace;
import org.openlumify.core.model.workspace.WorkspaceHelper;
import org.openlumify.core.model.workspace.WorkspaceRepository;
import org.openlumify.core.security.VisibilityTranslator;
import org.openlumify.core.user.User;
import org.openlumify.core.util.ClientApiConverter;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;
import org.openlumify.web.BadRequestException;
import org.openlumify.web.clientapi.model.ClientApiArtifactImportResponse;
import org.openlumify.web.clientapi.model.ClientApiImportProperty;
import org.openlumify.web.clientapi.model.ClientApiLongRunningProcessSubmitResponse;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;
import org.openlumify.web.util.HttpPartUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
public class VertexCloudImport implements ParameterizedHandler {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(VertexCloudImport.class);

    private final Graph graph;
    private final FileImport fileImport;
    private final WorkspaceRepository workspaceRepository;
    private final LongRunningProcessRepository longRunningProcessRepository;
    private final VisibilityTranslator visibilityTranslator;
    private final WorkspaceHelper workspaceHelper;
    private Authorizations authorizations;

    @Inject
    public VertexCloudImport(
            Graph graph,
            FileImport fileImport,
            WorkspaceRepository workspaceRepository,
            VisibilityTranslator visibilityTranslator,
            LongRunningProcessRepository longRunningProcessRepository,
            WorkspaceHelper workspaceHelper
    ) {
        this.graph = graph;
        this.fileImport = fileImport;
        this.workspaceRepository = workspaceRepository;
        this.longRunningProcessRepository = longRunningProcessRepository;
        this.visibilityTranslator = visibilityTranslator;
        this.workspaceHelper = workspaceHelper;
    }

    @Handle
    public ClientApiLongRunningProcessSubmitResponse handle(
            @Required(name = "cloudResource") String cloudResource,
            @Required(name = "cloudConfiguration") String cloudConfiguration,
            @Optional(name = "publish", defaultValue = "false") boolean shouldPublish,
            @Optional(name = "findExistingByFileHash", defaultValue = "true") boolean findExistingByFileHash,
            @ActiveWorkspaceId String workspaceId,
            Authorizations authorizations,
            User user
    ) throws Exception {
        workspaceId = workspaceHelper.getWorkspaceIdOrNullIfPublish(workspaceId, shouldPublish, user);

        this.authorizations = authorizations;

        CloudImportLongRunningProcessQueueItem item = new CloudImportLongRunningProcessQueueItem(
            cloudResource,
            cloudConfiguration,
            user.getUserId(),
            workspaceId,
            authorizations
        );
        String id = this.longRunningProcessRepository.enqueue(item.toJson(), user, authorizations);

        return new ClientApiLongRunningProcessSubmitResponse(id);
    }

    public Graph getGraph() {
        return graph;
    }

    protected Authorizations getAuthorizations() {
        return authorizations;
    }
}
