package org.openlumify.web.routes.vertex;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Required;
import org.vertexium.*;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.model.properties.OpenLumifyProperties;
import org.openlumify.core.model.termMention.TermMentionRepository;
import org.openlumify.core.model.workspace.WorkspaceHelper;
import org.openlumify.core.util.SandboxStatusUtil;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;
import org.openlumify.web.BadRequestException;
import org.openlumify.web.OpenLumifyResponse;
import org.openlumify.web.clientapi.model.ClientApiSuccess;
import org.openlumify.web.clientapi.model.SandboxStatus;
import org.openlumify.web.clientapi.model.VisibilityJson;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;

import static org.vertexium.util.IterableUtils.singleOrDefault;

@Singleton
public class UnresolveTermEntity implements ParameterizedHandler {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(UnresolveTermEntity.class);
    private final TermMentionRepository termMentionRepository;
    private final Graph graph;
    private final WorkspaceHelper workspaceHelper;

    @Inject
    public UnresolveTermEntity(
            final TermMentionRepository termMentionRepository,
            final Graph graph,
            final WorkspaceHelper workspaceHelper
    ) {
        this.termMentionRepository = termMentionRepository;
        this.graph = graph;
        this.workspaceHelper = workspaceHelper;
    }

    @Handle
    public ClientApiSuccess handle(
            @Required(name = "termMentionId") String termMentionId,
            @ActiveWorkspaceId String workspaceId,
            Authorizations authorizations
    ) throws Exception {
        LOGGER.debug("UnresolveTermEntity (termMentionId: %s)", termMentionId);

        Vertex termMention = termMentionRepository.findById(termMentionId, authorizations);
        if (termMention == null) {
            throw new OpenLumifyResourceNotFoundException("Could not find term mention with id: " + termMentionId);
        }

        Vertex resolvedVertex = singleOrDefault(termMention.getVertices(Direction.OUT, OpenLumifyProperties.TERM_MENTION_LABEL_RESOLVED_TO, authorizations), null);
        if (resolvedVertex == null) {
            throw new OpenLumifyResourceNotFoundException("Could not find resolved vertex from term mention: " + termMentionId);
        }

        String edgeId = OpenLumifyProperties.TERM_MENTION_RESOLVED_EDGE_ID.getPropertyValue(termMention);
        Edge edge = graph.getEdge(edgeId, authorizations);
        if (edge == null) {
            throw new OpenLumifyResourceNotFoundException("Could not find edge " + edgeId + " from term mention: " + termMentionId);
        }

        SandboxStatus vertexSandboxStatus = SandboxStatusUtil.getSandboxStatus(resolvedVertex, workspaceId);
        SandboxStatus edgeSandboxStatus = SandboxStatusUtil.getSandboxStatus(edge, workspaceId);
        if (vertexSandboxStatus == SandboxStatus.PUBLIC && edgeSandboxStatus == SandboxStatus.PUBLIC) {
            throw new BadRequestException("Can not unresolve a public entity");
        }

        VisibilityJson visibilityJson;
        if (vertexSandboxStatus == SandboxStatus.PUBLIC) {
            visibilityJson = OpenLumifyProperties.VISIBILITY_JSON.getPropertyValue(edge);
            VisibilityJson.removeFromWorkspace(visibilityJson, workspaceId);
        } else {
            visibilityJson = OpenLumifyProperties.VISIBILITY_JSON.getPropertyValue(resolvedVertex);
            VisibilityJson.removeFromWorkspace(visibilityJson, workspaceId);
        }

        workspaceHelper.unresolveTerm(termMention, authorizations);
        return OpenLumifyResponse.SUCCESS;
    }
}
