package org.openlumify.web.routes.edge;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.webster.ParameterizedHandler;
import org.openlumify.webster.annotations.Handle;
import org.openlumify.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Edge;
import org.vertexium.Graph;
import org.openlumify.core.config.Configuration;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.model.ontology.OntologyProperty;
import org.openlumify.core.model.ontology.OntologyRepository;
import org.openlumify.core.model.properties.OpenLumifyProperties;
import org.openlumify.core.model.workspace.WorkspaceHelper;
import org.openlumify.core.security.ACLProvider;
import org.openlumify.core.user.User;
import org.openlumify.web.OpenLumifyResponse;
import org.openlumify.web.clientapi.model.ClientApiSuccess;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;

import javax.servlet.http.HttpServletRequest;

@Singleton
public class EdgeDeleteProperty implements ParameterizedHandler {
    private final Graph graph;
    private final OntologyRepository ontologyRepository;
    private final WorkspaceHelper workspaceHelper;
    private final ACLProvider aclProvider;
    private final boolean autoPublishComments;

    @Inject
    public EdgeDeleteProperty(
            final Graph graph,
            final WorkspaceHelper workspaceHelper,
            final OntologyRepository ontologyRepository,
            final ACLProvider aclProvider,
            final Configuration configuration
    ) {
        this.graph = graph;
        this.workspaceHelper = workspaceHelper;
        this.ontologyRepository = ontologyRepository;
        this.aclProvider = aclProvider;
        this.autoPublishComments = configuration.getBoolean(Configuration.COMMENTS_AUTO_PUBLISH,
                Configuration.DEFAULT_COMMENTS_AUTO_PUBLISH);
    }

    @Handle
    public ClientApiSuccess handle(
            HttpServletRequest request,
            @Required(name = "edgeId") String edgeId,
            @Required(name = "propertyKey") String propertyKey,
            @Required(name = "propertyName") String propertyName,
            @ActiveWorkspaceId String workspaceId,
            User user,
            Authorizations authorizations
    ) throws Exception {
        OntologyProperty ontologyProperty = ontologyRepository.getRequiredPropertyByIRI(propertyName, workspaceId);

        // TODO remove all properties from all edges? I don't think so
        Edge edge = graph.getEdge(edgeId, authorizations);

        aclProvider.checkCanDeleteProperty(edge, propertyKey, propertyName, user, workspaceId);

        boolean isComment = OpenLumifyProperties.COMMENT.isSameName(propertyName);

        if (isComment && request.getPathInfo().equals("/edge/property")) {
            throw new OpenLumifyException("Use /edge/comment to save comment properties");
        } else if (!isComment && request.getPathInfo().equals("/edge/comment")) {
            throw new OpenLumifyException("Use /edge/property to save non-comment properties");
        }

        if (isComment && autoPublishComments) {
            workspaceId = null;
        }

        workspaceHelper.deleteProperties(edge, propertyKey, propertyName, ontologyProperty, workspaceId, authorizations,
                user);

        return OpenLumifyResponse.SUCCESS;
    }
}
