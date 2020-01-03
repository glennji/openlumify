package org.openlumify.web.routes.vertex;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Optional;
import org.visallo.webster.annotations.Required;
import org.vertexium.*;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.model.termMention.TermMentionRepository;
import org.openlumify.core.security.OpenLumifyVisibility;
import org.openlumify.core.security.VisibilityTranslator;
import org.openlumify.core.user.User;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;
import org.openlumify.web.clientapi.model.ClientApiSourceInfo;
import org.openlumify.web.clientapi.model.ClientApiVertexPropertyDetails;
import org.openlumify.web.clientapi.model.VisibilityJson;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;
import org.openlumify.web.util.VisibilityValidator;

import java.util.ResourceBundle;

@Singleton
public class VertexPropertyDetails implements ParameterizedHandler {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(VertexPropertyDetails.class);
    private final Graph graph;
    private final VisibilityTranslator visibilityTranslator;
    private final TermMentionRepository termMentionRepository;

    @Inject
    public VertexPropertyDetails(
            Graph graph,
            VisibilityTranslator visibilityTranslator,
            TermMentionRepository termMentionRepository
    ) {
        this.graph = graph;
        this.visibilityTranslator = visibilityTranslator;
        this.termMentionRepository = termMentionRepository;
    }

    @Handle
    public ClientApiVertexPropertyDetails handle(
            @Required(name = "vertexId") String vertexId,
            @Optional(name = "propertyKey") String propertyKey,
            @Required(name = "propertyName") String propertyName,
            @Required(name = "visibilitySource") String visibilitySource,
            @ActiveWorkspaceId String workspaceId,
            ResourceBundle resourceBundle,
            User user,
            Authorizations authorizations
    ) throws Exception {
        Visibility visibility = VisibilityValidator.validate(
                graph,
                visibilityTranslator,
                resourceBundle,
                visibilitySource,
                user,
                authorizations
        );

        Vertex vertex = this.graph.getVertex(vertexId, authorizations);
        if (vertex == null) {
            throw new OpenLumifyResourceNotFoundException("Could not find vertex with id: " + vertexId, vertexId);
        }

        Property property = vertex.getProperty(propertyKey, propertyName, visibility);
        if (property == null) {
            VisibilityJson visibilityJson = new VisibilityJson();
            visibilityJson.setSource(visibilitySource);
            visibilityJson.addWorkspace(workspaceId);
            OpenLumifyVisibility v2 = visibilityTranslator.toVisibility(visibilityJson);
            property = vertex.getProperty(propertyKey, propertyName, v2.getVisibility());
            if (property == null) {
                throw new OpenLumifyResourceNotFoundException("Could not find property " + propertyKey + ":" + propertyName + ":" + visibility + " on vertex with id: " + vertexId, vertexId);
            }
        }

        ClientApiSourceInfo sourceInfo = termMentionRepository.getSourceInfoForVertexProperty(vertex.getId(), property, authorizations);

        ClientApiVertexPropertyDetails result = new ClientApiVertexPropertyDetails();
        result.sourceInfo = sourceInfo;
        return result;
    }
}
