package org.openlumify.web.routes.vertex;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Required;
import org.apache.commons.io.IOUtils;
import org.vertexium.*;
import org.vertexium.mutation.ElementMutation;
import org.vertexium.property.StreamingPropertyValue;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.model.ontology.Concept;
import org.openlumify.core.model.ontology.OntologyRepository;
import org.openlumify.core.model.properties.OpenLumifyProperties;
import org.openlumify.core.model.workQueue.Priority;
import org.openlumify.core.model.workQueue.WorkQueueRepository;
import org.openlumify.core.model.workspace.Workspace;
import org.openlumify.core.model.workspace.WorkspaceRepository;
import org.openlumify.core.security.VisibilityTranslator;
import org.openlumify.core.user.User;
import org.openlumify.core.util.*;
import org.openlumify.web.clientapi.model.ClientApiVertex;
import org.openlumify.web.clientapi.model.VisibilityJson;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.vertexium.util.IterableUtils.toList;
import static org.openlumify.core.model.ontology.OntologyRepository.PUBLIC;

@Singleton
public class VertexUploadImage implements ParameterizedHandler {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(VertexUploadImage.class);
    private static final String SOURCE_UPLOAD = "User Upload";
    private static final String PROCESS = VertexUploadImage.class.getName();
    private static final String MULTI_VALUE_KEY = VertexUploadImage.class.getName();

    private final Graph graph;
    private final OntologyRepository ontologyRepository;
    private final WorkQueueRepository workQueueRepository;
    private final VisibilityTranslator visibilityTranslator;
    private final WorkspaceRepository workspaceRepository;
    private final String clockwiseRotationIri;
    private final String yAxisFlippedIri;
    private final String conceptIri;
    private final String entityHasImageIri;

    @Inject
    public VertexUploadImage(
            final Graph graph,
            final OntologyRepository ontologyRepository,
            final WorkQueueRepository workQueueRepository,
            final VisibilityTranslator visibilityTranslator,
            final WorkspaceRepository workspaceRepository
    ) {
        this.graph = graph;
        this.ontologyRepository = ontologyRepository;
        this.workQueueRepository = workQueueRepository;
        this.visibilityTranslator = visibilityTranslator;
        this.workspaceRepository = workspaceRepository;

        this.conceptIri = ontologyRepository.getRequiredConceptIRIByIntent("entityImage", PUBLIC);
        this.entityHasImageIri = ontologyRepository.getRequiredRelationshipIRIByIntent("entityHasImage", PUBLIC);
        this.yAxisFlippedIri = ontologyRepository.getRequiredPropertyIRIByIntent("media.yAxisFlipped", PUBLIC);
        this.clockwiseRotationIri = ontologyRepository.getRequiredPropertyIRIByIntent("media.clockwiseRotation", PUBLIC);
    }

    @Handle
    public ClientApiVertex handle(
            HttpServletRequest request,
            @Required(name = "graphVertexId") String graphVertexId,
            @ActiveWorkspaceId String workspaceId,
            User user,
            Authorizations authorizations
    ) throws Exception {
        final List<Part> files = Lists.newArrayList(request.getParts());

        Concept concept = ontologyRepository.getConceptByIRI(conceptIri, workspaceId);
        checkNotNull(concept, "Could not find image concept: " + conceptIri);

        if (files.size() != 1) {
            throw new RuntimeException("Wrong number of uploaded files. Expected 1 got " + files.size());
        }

        final Part file = files.get(0);
        Workspace workspace = this.workspaceRepository.findById(workspaceId, user);

        Vertex entityVertex = graph.getVertex(graphVertexId, authorizations);
        if (entityVertex == null) {
            throw new OpenLumifyResourceNotFoundException(String.format("Could not find associated entity vertex for id: %s", graphVertexId));
        }
        ElementMutation<Vertex> entityVertexMutation = entityVertex.prepareMutation();

        VisibilityJson visibilityJson = getOpenLumifyVisibility(entityVertex, workspaceId);
        Visibility visibility = visibilityTranslator.toVisibility(visibilityJson).getVisibility();

        Metadata metadata = new Metadata();
        OpenLumifyProperties.VISIBILITY_JSON_METADATA.setMetadata(metadata, visibilityJson, visibilityTranslator.getDefaultVisibility());
        OpenLumifyProperties.MODIFIED_DATE_METADATA.setMetadata(metadata, new Date(), visibilityTranslator.getDefaultVisibility());
        OpenLumifyProperties.MODIFIED_BY_METADATA.setMetadata(metadata, user.getUserId(), visibilityTranslator.getDefaultVisibility());

        String title = imageTitle(entityVertex, workspaceId);
        ElementBuilder<Vertex> artifactVertexBuilder = convertToArtifact(file, title, visibilityJson, metadata, user, visibility);
        Vertex artifactVertex = artifactVertexBuilder.save(authorizations);
        this.graph.flush();

        entityVertexMutation.setProperty(OpenLumifyProperties.ENTITY_IMAGE_VERTEX_ID.getPropertyName(), artifactVertex.getId(), metadata, visibility);
        entityVertex = entityVertexMutation.save(authorizations);
        graph.flush();

        List<Edge> existingEdges = toList(entityVertex.getEdges(artifactVertex, Direction.BOTH, entityHasImageIri, authorizations));
        if (existingEdges.size() == 0) {
            EdgeBuilder edgeBuilder = graph.prepareEdge(entityVertex, artifactVertex, entityHasImageIri, visibility);
            Visibility defaultVisibility = visibilityTranslator.getDefaultVisibility();
            OpenLumifyProperties.VISIBILITY_JSON.setProperty(edgeBuilder, visibilityJson, defaultVisibility);
            OpenLumifyProperties.MODIFIED_DATE.setProperty(edgeBuilder, new Date(), defaultVisibility);
            OpenLumifyProperties.MODIFIED_BY.setProperty(edgeBuilder, user.getUserId(), defaultVisibility);
            edgeBuilder.save(authorizations);
        }

        this.workspaceRepository.updateEntityOnWorkspace(workspace, artifactVertex.getId(), user);
        this.workspaceRepository.updateEntityOnWorkspace(workspace, entityVertex.getId(), user);

        graph.flush();

        workQueueRepository.pushElement(artifactVertex, Priority.HIGH);
        workQueueRepository.pushGraphPropertyQueue(
                artifactVertex,
                null,
                OpenLumifyProperties.RAW.getPropertyName(),
                workspaceId,
                visibilityJson.getSource(),
                Priority.HIGH
        );
        workQueueRepository.pushElementImageQueue(
                entityVertex,
                null,
                OpenLumifyProperties.ENTITY_IMAGE_VERTEX_ID.getPropertyName(),
                Priority.HIGH
        );

        return (ClientApiVertex) ClientApiConverter.toClientApi(entityVertex, workspaceId, authorizations);
    }

    private String imageTitle(Vertex entityVertex, String workspaceId) {
        Property titleProperty = OpenLumifyProperties.TITLE.getFirstProperty(entityVertex);
        Object title;
        if (titleProperty == null) {
            String conceptTypeProperty = OpenLumifyProperties.CONCEPT_TYPE.getPropertyName();
            String vertexConceptType = (String) entityVertex.getProperty(conceptTypeProperty).getValue();
            Concept concept = ontologyRepository.getConceptByIRI(vertexConceptType, workspaceId);
            title = concept.getDisplayName();
        } else {
            title = titleProperty.getValue();
        }
        return String.format("Image of %s", title.toString());
    }

    private VisibilityJson getOpenLumifyVisibility(Vertex entityVertex, String workspaceId) {
        VisibilityJson visibilityJson = OpenLumifyProperties.VISIBILITY_JSON.getPropertyValue(entityVertex);
        if (visibilityJson == null) {
            visibilityJson = new VisibilityJson();
        }
        String visibilitySource = visibilityJson.getSource();
        if (visibilitySource == null) {
            visibilitySource = "";
        }
        return VisibilityJson.updateVisibilitySourceAndAddWorkspaceId(visibilityJson, visibilitySource, workspaceId);
    }

    protected ElementBuilder<Vertex> convertToArtifact(
            final Part file,
            String title,
            VisibilityJson visibilityJson,
            Metadata metadata,
            User user,
            Visibility visibility
    ) throws IOException {
        Visibility defaultVisibility = visibilityTranslator.getDefaultVisibility();
        final InputStream fileInputStream = file.getInputStream();
        final byte[] rawContent = IOUtils.toByteArray(fileInputStream);
        LOGGER.debug("Uploaded file raw content byte length: %d", rawContent.length);

        final String fileName = file.getName();

        final String fileRowKey = RowKeyHelper.buildSHA256KeyString(rawContent);
        LOGGER.debug("Generated row key: %s", fileRowKey);

        StreamingPropertyValue rawValue = StreamingPropertyValue.create(new ByteArrayInputStream(rawContent), byte[].class);
        rawValue.searchIndex(false);
        rawValue.store(true);

        ElementBuilder<Vertex> vertexBuilder = graph.prepareVertex(visibility);
        // Note that OpenLumifyProperties.MIME_TYPE is expected to be set by a GraphPropertyWorker.
        OpenLumifyProperties.CONCEPT_TYPE.setProperty(vertexBuilder, conceptIri, defaultVisibility);
        OpenLumifyProperties.VISIBILITY_JSON.setProperty(vertexBuilder, visibilityJson, defaultVisibility);
        OpenLumifyProperties.MODIFIED_BY.setProperty(vertexBuilder, user.getUserId(), defaultVisibility);
        OpenLumifyProperties.MODIFIED_DATE.setProperty(vertexBuilder, new Date(), defaultVisibility);
        OpenLumifyProperties.TITLE.addPropertyValue(vertexBuilder, MULTI_VALUE_KEY, title, metadata, visibility);
        OpenLumifyProperties.FILE_NAME.addPropertyValue(vertexBuilder, MULTI_VALUE_KEY, fileName, metadata, visibility);
        OpenLumifyProperties.RAW.setProperty(vertexBuilder, rawValue, metadata, visibility);
        OpenLumifyProperties.SOURCE.addPropertyValue(vertexBuilder, MULTI_VALUE_KEY, SOURCE_UPLOAD, metadata, visibility);
        OpenLumifyProperties.PROCESS.addPropertyValue(vertexBuilder, MULTI_VALUE_KEY, PROCESS, metadata, visibility);

        ImageTransform imageTransform = ImageTransformExtractor.getImageTransform(rawContent);
        vertexBuilder.setProperty(yAxisFlippedIri, imageTransform.isYAxisFlipNeeded(), metadata, visibility);
        vertexBuilder.setProperty(clockwiseRotationIri, imageTransform.getCWRotationNeeded(), metadata, visibility);

        return vertexBuilder;
    }
}
