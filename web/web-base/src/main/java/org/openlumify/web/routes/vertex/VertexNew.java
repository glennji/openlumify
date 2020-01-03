package org.openlumify.web.routes.vertex;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Optional;
import org.visallo.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Metadata;
import org.vertexium.Vertex;
import org.openlumify.core.model.graph.GraphRepository;
import org.openlumify.core.model.graph.VisibilityAndElementMutation;
import org.openlumify.core.model.ontology.OntologyProperty;
import org.openlumify.core.model.ontology.OntologyRepository;
import org.openlumify.core.model.properties.OpenLumifyProperties;
import org.openlumify.core.model.workQueue.Priority;
import org.openlumify.core.model.workQueue.WorkQueueRepository;
import org.openlumify.core.model.workspace.WorkspaceHelper;
import org.openlumify.core.model.workspace.WorkspaceRepository;
import org.openlumify.core.security.VisibilityTranslator;
import org.openlumify.core.user.User;
import org.openlumify.core.util.ClientApiConverter;
import org.openlumify.core.util.VertexiumMetadataUtil;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;
import org.openlumify.web.clientapi.model.ClientApiAddElementProperties;
import org.openlumify.web.clientapi.model.ClientApiSourceInfo;
import org.openlumify.web.clientapi.model.ClientApiVertex;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;
import org.openlumify.web.parameterProviders.JustificationText;
import org.openlumify.web.util.VisibilityValidator;

import java.util.Arrays;
import java.util.ResourceBundle;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class VertexNew implements ParameterizedHandler {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(VertexNew.class);

    private final Graph graph;
    private final VisibilityTranslator visibilityTranslator;
    private final WorkspaceRepository workspaceRepository;
    private final WorkQueueRepository workQueueRepository;
    private final OntologyRepository ontologyRepository;
    private final GraphRepository graphRepository;
    private final WorkspaceHelper workspaceHelper;

    @Inject
    public VertexNew(
            Graph graph,
            VisibilityTranslator visibilityTranslator,
            WorkspaceRepository workspaceRepository,
            WorkQueueRepository workQueueRepository,
            OntologyRepository ontologyRepository,
            GraphRepository graphRepository,
            WorkspaceHelper workspaceHelper
    ) {
        this.graph = graph;
        this.visibilityTranslator = visibilityTranslator;
        this.workspaceRepository = workspaceRepository;
        this.workQueueRepository = workQueueRepository;
        this.ontologyRepository = ontologyRepository;
        this.graphRepository = graphRepository;
        this.workspaceHelper = workspaceHelper;
    }

    @Handle
    public ClientApiVertex handle(
            @Optional(name = "vertexId", allowEmpty = false) String vertexId,
            @Required(name = "conceptType", allowEmpty = false) String conceptType,
            @Required(name = "visibilitySource") String visibilitySource,
            @Optional(name = "properties", allowEmpty = false) String propertiesJsonString,
            @Optional(name = "publish", defaultValue = "false") boolean shouldPublish,
            @JustificationText String justificationText,
            ClientApiSourceInfo sourceInfo,
            @ActiveWorkspaceId(required = false) String workspaceId,
            ResourceBundle resourceBundle,
            User user,
            Authorizations authorizations
    ) throws Exception {
        VisibilityValidator.validate(graph, visibilityTranslator, resourceBundle, visibilitySource, user, authorizations);

        workspaceId = workspaceHelper.getWorkspaceIdOrNullIfPublish(workspaceId, shouldPublish, user);

        Vertex vertex = graphRepository.addVertex(
                vertexId,
                conceptType,
                visibilitySource,
                workspaceId,
                justificationText,
                sourceInfo,
                user,
                authorizations
        );

        ClientApiAddElementProperties properties = null;
        if (propertiesJsonString != null && propertiesJsonString.length() > 0) {
            properties = ClientApiConverter.toClientApi(propertiesJsonString, ClientApiAddElementProperties.class);
            for (ClientApiAddElementProperties.Property property : properties.properties) {
                OntologyProperty ontologyProperty = ontologyRepository.getPropertyByIRI(property.propertyName, workspaceId);
                checkNotNull(ontologyProperty, "Could not find ontology property '" + property.propertyName + "'");
                Object value = ontologyProperty.convertString(property.value);
                Metadata metadata = VertexiumMetadataUtil.metadataStringToMap(
                        property.metadataString,
                        this.visibilityTranslator.getDefaultVisibility()
                );
                VisibilityAndElementMutation<Vertex> setPropertyResult = graphRepository.setProperty(
                        vertex,
                        property.propertyName,
                        property.propertyKey,
                        value,
                        metadata,
                        null,
                        property.visibilitySource,
                        workspaceId,
                        justificationText,
                        sourceInfo,
                        user,
                        authorizations
                );
                setPropertyResult.elementMutation.save(authorizations);
            }
        }
        this.graph.flush();

        LOGGER.debug("Created new empty vertex with id: %s", vertex.getId());

        workQueueRepository.broadcastElement(vertex, workspaceId);
        workQueueRepository.pushGraphPropertyQueue(
                vertex,
                null,
                OpenLumifyProperties.CONCEPT_TYPE.getPropertyName(),
                workspaceId,
                visibilitySource,
                Priority.HIGH
        );

        if (workspaceId != null) {
            workspaceHelper.updateEntitiesOnWorkspace(
                    workspaceId,
                    Arrays.asList(vertex.getId()),
                    user
            );
        }

        if (properties != null) {
            for (ClientApiAddElementProperties.Property property : properties.properties) {
                workQueueRepository.pushGraphPropertyQueue(
                        vertex,
                        property.propertyKey,
                        property.propertyName,
                        workspaceId,
                        property.visibilitySource,
                        Priority.HIGH
                );
            }
        }

        return (ClientApiVertex) ClientApiConverter.toClientApi(vertex, workspaceId, authorizations);
    }
}
