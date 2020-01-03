package org.openlumify.web.routes.edge;

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
import org.openlumify.web.clientapi.model.ClientApiEdgePropertyDetails;
import org.openlumify.web.clientapi.model.ClientApiSourceInfo;
import org.openlumify.web.clientapi.model.VisibilityJson;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;
import org.openlumify.web.util.VisibilityValidator;

import java.util.ResourceBundle;

@Singleton
public class EdgePropertyDetails implements ParameterizedHandler {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(EdgePropertyDetails.class);
    private final Graph graph;
    private final TermMentionRepository termMentionRepository;
    private final VisibilityTranslator visibilityTranslator;

    @Inject
    public EdgePropertyDetails(
            Graph graph,
            TermMentionRepository termMentionRepository,
            VisibilityTranslator visibilityTranslator
    ) {
        this.graph = graph;
        this.termMentionRepository = termMentionRepository;
        this.visibilityTranslator = visibilityTranslator;
    }

    @Handle
    public ClientApiEdgePropertyDetails handle(
            @Required(name = "edgeId") String edgeId,
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

        Edge edge = this.graph.getEdge(edgeId, authorizations);
        if (edge == null) {
            throw new OpenLumifyResourceNotFoundException("Could not find edge with id: " + edgeId, edgeId);
        }

        Property property = edge.getProperty(propertyKey, propertyName, visibility);
        if (property == null) {
            VisibilityJson visibilityJson = new VisibilityJson();
            visibilityJson.setSource(visibilitySource);
            visibilityJson.addWorkspace(workspaceId);
            OpenLumifyVisibility v2 = visibilityTranslator.toVisibility(visibilityJson);
            property = edge.getProperty(propertyKey, propertyName, v2.getVisibility());
            if (property == null) {
                throw new OpenLumifyResourceNotFoundException("Could not find property " + propertyKey + ":" + propertyName + ":" + visibility + " on edge with id: " + edgeId, edgeId);
            }
        }

        ClientApiSourceInfo sourceInfo = termMentionRepository.getSourceInfoForEdgeProperty(edge, property, authorizations);

        ClientApiEdgePropertyDetails result = new ClientApiEdgePropertyDetails();
        result.sourceInfo = sourceInfo;
        return result;
    }
}
