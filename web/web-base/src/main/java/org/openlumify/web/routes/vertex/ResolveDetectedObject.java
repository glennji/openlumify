package org.openlumify.web.routes.vertex;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.webster.ParameterizedHandler;
import org.openlumify.webster.annotations.Handle;
import org.openlumify.webster.annotations.Optional;
import org.openlumify.webster.annotations.Required;
import org.vertexium.*;
import org.openlumify.core.ingest.ArtifactDetectedObject;
import org.openlumify.core.model.graph.GraphRepository;
import org.openlumify.core.model.graph.GraphUpdateContext;
import org.openlumify.core.model.ontology.Concept;
import org.openlumify.core.model.ontology.OntologyRepository;
import org.openlumify.core.model.properties.OpenLumifyProperties;
import org.openlumify.core.model.properties.types.PropertyMetadata;
import org.openlumify.core.model.termMention.TermMentionRepository;
import org.openlumify.core.model.workQueue.Priority;
import org.openlumify.core.model.workQueue.WorkQueueRepository;
import org.openlumify.core.model.workspace.Workspace;
import org.openlumify.core.model.workspace.WorkspaceRepository;
import org.openlumify.core.security.OpenLumifyVisibility;
import org.openlumify.core.security.VisibilityTranslator;
import org.openlumify.core.user.User;
import org.openlumify.core.util.ClientApiConverter;
import org.openlumify.web.clientapi.model.ClientApiSourceInfo;
import org.openlumify.web.clientapi.model.ClientApiVertex;
import org.openlumify.web.clientapi.model.VisibilityJson;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;
import org.openlumify.web.parameterProviders.JustificationText;
import org.openlumify.web.util.VisibilityValidator;

import java.util.Date;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;

@Singleton
public class ResolveDetectedObject implements ParameterizedHandler {
    private static final String MULTI_VALUE_KEY_PREFIX = ResolveDetectedObject.class.getName();
    private static final String MULTI_VALUE_KEY = ResolveDetectedObject.class.getName();
    private final Graph graph;
    private final OntologyRepository ontologyRepository;
    private final WorkQueueRepository workQueueRepository;
    private final VisibilityTranslator visibilityTranslator;
    private final WorkspaceRepository workspaceRepository;
    private final TermMentionRepository termMentionRepository;
    private final GraphRepository graphRepository;

    @Inject
    public ResolveDetectedObject(
            final Graph graph,
            final OntologyRepository ontologyRepository,
            final WorkQueueRepository workQueueRepository,
            final VisibilityTranslator visibilityTranslator,
            final WorkspaceRepository workspaceRepository,
            final TermMentionRepository termMentionRepository,
            final GraphRepository graphRepository
    ) {
        this.graph = graph;
        this.ontologyRepository = ontologyRepository;
        this.workQueueRepository = workQueueRepository;
        this.visibilityTranslator = visibilityTranslator;
        this.workspaceRepository = workspaceRepository;
        this.termMentionRepository = termMentionRepository;
        this.graphRepository = graphRepository;
    }

    @Handle
    public ClientApiVertex handle(
            @Required(name = "artifactId") String artifactId,
            @Required(name = "title") String title,
            @Required(name = "conceptId") String conceptId,
            @Required(name = "visibilitySource") String visibilitySource,
            @Optional(name = "graphVertexId") String graphVertexId,
            @JustificationText String justificationText,
            @Optional(name = "sourceInfo") String sourceInfoString,
            @Optional(name = "originalPropertyKey") String originalPropertyKey,
            @Required(name = "x1") double x1,
            @Required(name = "x2") double x2,
            @Required(name = "y1") double y1,
            @Required(name = "y2") double y2,
            ResourceBundle resourceBundle,
            @ActiveWorkspaceId String workspaceId,
            User user,
            Authorizations authorizations
    ) throws Exception {
        String artifactContainsImageOfEntityIri = ontologyRepository.getRequiredRelationshipIRIByIntent("artifactContainsImageOfEntity", workspaceId);
        Concept concept = ontologyRepository.getConceptByIRI(conceptId, workspaceId);

        Workspace workspace = workspaceRepository.findById(workspaceId, user);

        VisibilityValidator.validate(graph, visibilityTranslator, resourceBundle, visibilitySource, user, authorizations);

        VisibilityJson visibilityJson = VisibilityJson.updateVisibilitySourceAndAddWorkspaceId(null, visibilitySource, workspaceId);
        OpenLumifyVisibility openlumifyVisibility = visibilityTranslator.toVisibility(visibilityJson);
        Visibility visibility = openlumifyVisibility.getVisibility();

        Date modifiedDate = new Date();

        PropertyMetadata propertyMetadata = new PropertyMetadata(modifiedDate, user, visibilityJson, visibility);
        String id = graphVertexId == null ? graph.getIdGenerator().nextId() : graphVertexId;

        Vertex artifactVertex = graph.getVertex(artifactId, authorizations);

        Edge edge;
        Vertex resolvedVertex;
        ArtifactDetectedObject artifactDetectedObject;
        String propertyKey;
        final AtomicBoolean isNewVertex = new AtomicBoolean(false);
        try (GraphUpdateContext ctx = graphRepository.beginGraphUpdate(Priority.NORMAL, user, authorizations)) {
            ctx.setPushOnQueue(false);
            edge = ctx.getOrCreateEdgeAndUpdate(null, artifactId, id, artifactContainsImageOfEntityIri, visibility, edgeCtx -> {
                edgeCtx.updateBuiltInProperties(propertyMetadata);
            }).get();

            artifactDetectedObject = new ArtifactDetectedObject(
                    x1,
                    y1,
                    x2,
                    y2,
                    concept.getIRI(),
                    "user",
                    edge.getId(),
                    id,
                    originalPropertyKey
            );

            propertyKey = artifactDetectedObject.getMultivalueKey(MULTI_VALUE_KEY_PREFIX);
            resolvedVertex = ctx.getOrCreateVertexAndUpdate(id, visibility, elemCtx -> {
                if (elemCtx.isNewElement()) {
                    isNewVertex.set(true);
                    elemCtx.setConceptType(concept.getIRI());
                    elemCtx.updateBuiltInProperties(propertyMetadata);
                    OpenLumifyProperties.TITLE.updateProperty(elemCtx, MULTI_VALUE_KEY, title, propertyMetadata);
                }

                OpenLumifyProperties.ROW_KEY.updateProperty(elemCtx, id, propertyKey, propertyMetadata);
            }).get();

            if (isNewVertex.get()) {
                ClientApiSourceInfo sourceInfo = ClientApiSourceInfo.fromString(sourceInfoString);
                termMentionRepository.addJustification(resolvedVertex, justificationText, sourceInfo, openlumifyVisibility, authorizations);
                workspaceRepository.updateEntityOnWorkspace(workspace, resolvedVertex.getId(), user);
            }
            OpenLumifyProperties.DETECTED_OBJECT.addPropertyValue(artifactVertex, propertyKey, artifactDetectedObject, openlumifyVisibility.getVisibility(), authorizations);
        }

        workQueueRepository.broadcastElement(edge, workspaceId);
        workQueueRepository.pushGraphPropertyQueue(artifactVertex, propertyKey, OpenLumifyProperties.DETECTED_OBJECT.getPropertyName(), Priority.HIGH);

        return (ClientApiVertex) ClientApiConverter.toClientApi(artifactVertex, workspaceId, authorizations);
    }
}
