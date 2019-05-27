package org.openlumify.core.model.graph;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.vertexium.*;
import org.vertexium.mutation.ExistingElementMutation;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.ingest.graphProperty.ElementOrPropertyStatus;
import org.openlumify.core.model.PropertyJustificationMetadata;
import org.openlumify.core.model.properties.OpenLumifyProperties;
import org.openlumify.core.model.termMention.TermMentionFor;
import org.openlumify.core.model.termMention.TermMentionRepository;
import org.openlumify.core.model.workQueue.Priority;
import org.openlumify.core.model.workQueue.WorkQueueRepository;
import org.openlumify.core.security.OpenLumifyVisibility;
import org.openlumify.core.security.VisibilityTranslator;
import org.openlumify.core.user.User;
import org.openlumify.core.util.VertexiumMetadataUtil;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;
import org.openlumify.web.clientapi.model.ClientApiSourceInfo;
import org.openlumify.web.clientapi.model.SandboxStatus;
import org.openlumify.web.clientapi.model.VisibilityJson;

import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class GraphRepository {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(GraphRepository.class);
    public static final String VISALLO_VERSION_KEY = "openlumify.version";
    public static final int VISALLO_VERSION = 3;
    public static final double SET_PROPERTY_CONFIDENCE = 0.5;
    private final Graph graph;
    private final VisibilityTranslator visibilityTranslator;
    private final TermMentionRepository termMentionRepository;
    private final WorkQueueRepository workQueueRepository;

    @Inject
    public GraphRepository(
            Graph graph,
            VisibilityTranslator visibilityTranslator,
            TermMentionRepository termMentionRepository,
            WorkQueueRepository workQueueRepository
    ) {
        this.graph = graph;
        this.visibilityTranslator = visibilityTranslator;
        this.termMentionRepository = termMentionRepository;
        this.workQueueRepository = workQueueRepository;
    }

    public void verifyVersion() {
        verifyVersion(VISALLO_VERSION);
    }

    public void verifyVersion(int requiredVersion) {
        Object version = graph.getMetadata(VISALLO_VERSION_KEY);
        if (version == null) {
            writeVersion();
            return;
        }
        if (!(version instanceof Integer)) {
            throw new OpenLumifyException("Invalid " + VISALLO_VERSION_KEY + " found. Expected Integer, found " + version.getClass().getName());
        }
        Integer versionInt = (Integer) version;
        if (versionInt != requiredVersion) {
            throw new OpenLumifyException("Invalid " + VISALLO_VERSION_KEY + " found. Expected " + requiredVersion + ", found " + versionInt);
        }
        LOGGER.info("OpenLumify graph version verified: %d", versionInt);
    }

    public void writeVersion() {
        writeVersion(VISALLO_VERSION);
    }

    public void writeVersion(int version) {
        graph.setMetadata(VISALLO_VERSION_KEY, version);
        LOGGER.info("Wrote %s: %d", VISALLO_VERSION_KEY, version);
    }

    public <T extends Element> VisibilityAndElementMutation<T> updateElementVisibilitySource(
            Element element,
            SandboxStatus sandboxStatus,
            String visibilitySource,
            String workspaceId,
            Authorizations authorizations
    ) {
        Visibility defaultVisibility = visibilityTranslator.getDefaultVisibility();
        VisibilityJson visibilityJson = OpenLumifyProperties.VISIBILITY_JSON.getPropertyValue(element);
        visibilityJson = sandboxStatus != SandboxStatus.PUBLIC
                ? VisibilityJson.updateVisibilitySourceAndAddWorkspaceId(visibilityJson, visibilitySource, workspaceId)
                : VisibilityJson.updateVisibilitySource(visibilityJson, visibilitySource);

        OpenLumifyVisibility openlumifyVisibility = visibilityTranslator.toVisibility(visibilityJson);
        Visibility visibility = openlumifyVisibility.getVisibility();

        ExistingElementMutation<T> m = element.<T>prepareMutation().alterElementVisibility(visibility);
        OpenLumifyProperties.VISIBILITY_JSON.setProperty(m, visibilityJson, defaultVisibility);
        m.save(authorizations);
        return new VisibilityAndElementMutation<>(openlumifyVisibility, m);
    }

    public <T extends Element> Property updatePropertyVisibilitySource(
            Element element,
            String propertyKey,
            String propertyName,
            String oldVisibilitySource,
            String newVisibilitySource,
            String workspaceId,
            User user,
            Authorizations authorizations
    ) {
        Visibility defaultVisibility = visibilityTranslator.getDefaultVisibility();
        Property property = getProperty(element, propertyKey, propertyName, oldVisibilitySource, workspaceId);
        if (property == null) {
            throw new OpenLumifyResourceNotFoundException("Could not find property " + propertyKey + ":" + propertyName + " on element " + element.getId());
        }

        VisibilityJson newVisibilityJson = new VisibilityJson(newVisibilitySource);
        Visibility newVisibility = visibilityTranslator.toVisibility(newVisibilityJson).getVisibility();

        LOGGER.info(
                "%s Altering property visibility %s [%s:%s] from [%s] to [%s]",
                user.getUserId(),
                element.getId(),
                propertyKey,
                propertyName,
                oldVisibilitySource,
                newVisibility.toString()
        );

        ExistingElementMutation<T> m = element.<T>prepareMutation()
                .alterPropertyVisibility(property, newVisibility);
        OpenLumifyProperties.VISIBILITY_JSON_METADATA.setMetadata(m, property, newVisibilityJson, defaultVisibility);
        T newElement = m.save(authorizations);

        Property newProperty = newElement.getProperty(propertyKey, propertyName, newVisibility);
        checkNotNull(
                newProperty,
                "Could not find altered property " + propertyKey + ":" + propertyName + " on element " + element.getId()
        );

        return newProperty;
    }

    private Property getProperty(
            Element element,
            String propertyKey,
            String propertyName,
            String visibilitySource,
            String workspaceId
    ) {
        Property property = element.getProperty(
                propertyKey,
                propertyName,
                getVisibilityWithWorkspace(visibilitySource, workspaceId)
        );

        // could be a public property, let's try fetching it without workspace id
        if (property == null) {
            property = element.getProperty(
                    propertyKey,
                    propertyName,
                    getVisibilityWithWorkspace(visibilitySource, null)
            );
        }

        return property;
    }

    public <T extends Element> VisibilityAndElementMutation<T> setProperty(
            T element,
            String propertyName,
            String propertyKey,
            Object value,
            Metadata metadata,
            String oldVisibilitySource,
            String newVisibilitySource,
            String workspaceId,
            String justificationText,
            ClientApiSourceInfo sourceInfo,
            User user,
            Authorizations authorizations
    ) {
        Visibility defaultVisibility = visibilityTranslator.getDefaultVisibility();

        Visibility oldPropertyVisibility = getVisibilityWithWorkspace(oldVisibilitySource, workspaceId);
        Property oldProperty = element.getProperty(propertyKey, propertyName, oldPropertyVisibility);
        boolean isUpdate = oldProperty != null;

        Metadata propertyMetadata = isUpdate ? oldProperty.getMetadata() : new Metadata();
        propertyMetadata = VertexiumMetadataUtil.mergeMetadata(propertyMetadata, metadata);

        ExistingElementMutation<T> elementMutation = element.prepareMutation();

        VisibilityJson visibilityJson = VisibilityJson.updateVisibilitySourceAndAddWorkspaceId(
                null,
                newVisibilitySource,
                workspaceId
        );
        OpenLumifyProperties.VISIBILITY_JSON_METADATA.setMetadata(propertyMetadata, visibilityJson, defaultVisibility);
        OpenLumifyProperties.MODIFIED_DATE_METADATA.setMetadata(propertyMetadata, new Date(), defaultVisibility);
        OpenLumifyProperties.MODIFIED_BY_METADATA.setMetadata(propertyMetadata, user.getUserId(), defaultVisibility);
        OpenLumifyProperties.CONFIDENCE_METADATA.setMetadata(propertyMetadata, SET_PROPERTY_CONFIDENCE, defaultVisibility);

        OpenLumifyVisibility openlumifyVisibility = visibilityTranslator.toVisibility(visibilityJson);
        Visibility propertyVisibility = openlumifyVisibility.getVisibility();

        if (justificationText != null) {
            PropertyJustificationMetadata propertyJustificationMetadata = new PropertyJustificationMetadata(
                    justificationText);
            termMentionRepository.removeSourceInfoEdge(
                    element,
                    propertyKey,
                    propertyName,
                    openlumifyVisibility,
                    authorizations
            );
            OpenLumifyProperties.JUSTIFICATION_METADATA.setMetadata(
                    propertyMetadata,
                    propertyJustificationMetadata,
                    defaultVisibility
            );
        } else if (sourceInfo != null) {
            Vertex outVertex = graph.getVertex(sourceInfo.vertexId, authorizations);
            OpenLumifyProperties.JUSTIFICATION.removeMetadata(propertyMetadata);
            termMentionRepository.addSourceInfo(
                    element,
                    element.getId(),
                    TermMentionFor.PROPERTY,
                    propertyKey,
                    propertyName,
                    propertyVisibility,
                    sourceInfo.snippet,
                    sourceInfo.textPropertyKey,
                    sourceInfo.textPropertyName,
                    sourceInfo.startOffset,
                    sourceInfo.endOffset,
                    outVertex,
                    propertyVisibility,
                    authorizations
            );
        }

        Property publicProperty = element.getProperty(propertyKey, propertyName);
        // only public properties in a workspace will be sandboxed (hidden from the workspace)
        if (publicProperty != null && workspaceId != null &&
                SandboxStatus.getFromVisibilityString(publicProperty.getVisibility().getVisibilityString(), workspaceId)
                        == SandboxStatus.PUBLIC) {
            long beforeDeletionTimestamp = System.currentTimeMillis() - 1;
            // changing a public property, so hide it from the workspace
            element.markPropertyHidden(publicProperty, new Visibility(workspaceId), authorizations);
            graph.flush();
            workQueueRepository.pushGraphPropertyQueueHiddenOrDeleted(element, publicProperty, ElementOrPropertyStatus.HIDDEN, beforeDeletionTimestamp, workspaceId, Priority.HIGH);
        } else if (isUpdate && oldVisibilitySource != null && !oldVisibilitySource.equals(newVisibilitySource)) {
            // changing a existing sandboxed property's visibility
            elementMutation.alterPropertyVisibility(oldProperty, propertyVisibility);
        }

        elementMutation.addPropertyValue(propertyKey, propertyName, value, propertyMetadata, propertyVisibility);

        return new VisibilityAndElementMutation<>(openlumifyVisibility, elementMutation);
    }

    private Visibility getVisibilityWithWorkspace(String visibilitySource, String workspaceId) {
        Visibility visibility = null;
        if (visibilitySource != null) {
            VisibilityJson oldVisibilityJson = new VisibilityJson();
            oldVisibilityJson.setSource(visibilitySource);
            oldVisibilityJson.addWorkspace(workspaceId);
            visibility = visibilityTranslator.toVisibility(oldVisibilityJson).getVisibility();
        }
        return visibility;
    }

    public Vertex addVertex(
            String vertexId,
            String conceptType,
            String visibilitySource,
            String workspaceId,
            String justificationText,
            ClientApiSourceInfo sourceInfo,
            User user,
            Authorizations authorizations
    ) {
        VisibilityJson visibilityJson = VisibilityJson.updateVisibilitySourceAndAddWorkspaceId(
                null,
                visibilitySource,
                workspaceId
        );
        return addVertex(vertexId, conceptType, visibilityJson, justificationText, sourceInfo, user, authorizations);
    }

    public Vertex addVertex(
            String vertexId,
            String conceptType,
            VisibilityJson visibilityJson,
            String justificationText,
            ClientApiSourceInfo sourceInfo,
            User user,
            Authorizations authorizations
    ) {
        OpenLumifyVisibility openlumifyVisibility = visibilityTranslator.toVisibility(visibilityJson);
        VertexBuilder vertexBuilder;
        if (vertexId != null) {
            vertexBuilder = graph.prepareVertex(vertexId, openlumifyVisibility.getVisibility());
        } else {
            vertexBuilder = graph.prepareVertex(openlumifyVisibility.getVisibility());
        }
        updateElementMetadataProperties(vertexBuilder, conceptType, visibilityJson, user);

        boolean justificationAdded = addJustification(
                vertexBuilder,
                justificationText,
                openlumifyVisibility,
                visibilityJson,
                user
        );

        Vertex vertex = vertexBuilder.save(authorizations);
        graph.flush();

        if (justificationAdded) {
            termMentionRepository.removeSourceInfoEdgeFromVertex(
                    vertex.getId(),
                    vertex.getId(),
                    null,
                    null,
                    openlumifyVisibility,
                    authorizations
            );
        } else if (sourceInfo != null) {
            OpenLumifyProperties.JUSTIFICATION.removeProperty(vertexBuilder, openlumifyVisibility.getVisibility());

            Vertex sourceDataVertex = graph.getVertex(sourceInfo.vertexId, authorizations);
            termMentionRepository.addSourceInfoToVertex(
                    vertex,
                    vertex.getId(),
                    TermMentionFor.VERTEX,
                    null,
                    null,
                    null,
                    sourceInfo.snippet,
                    sourceInfo.textPropertyKey,
                    sourceInfo.textPropertyName,
                    sourceInfo.startOffset,
                    sourceInfo.endOffset,
                    sourceDataVertex,
                    openlumifyVisibility.getVisibility(),
                    authorizations
            );
        }

        return vertex;
    }

    public Edge addEdge(
            String edgeId,
            Vertex outVertex,
            Vertex inVertex,
            String predicateLabel,
            String justificationText,
            ClientApiSourceInfo sourceInfo,
            String visibilitySource,
            String workspaceId,
            User user,
            Authorizations authorizations
    ) {
        VisibilityJson visibilityJson = VisibilityJson.updateVisibilitySourceAndAddWorkspaceId(
                null,
                visibilitySource,
                workspaceId
        );
        return addEdge(edgeId, outVertex, inVertex, predicateLabel, justificationText, sourceInfo, visibilityJson, user, authorizations);
    }

    public Edge addEdge(
            String edgeId,
            Vertex outVertex,
            Vertex inVertex,
            String predicateLabel,
            String justificationText,
            ClientApiSourceInfo sourceInfo,
            VisibilityJson visibilityJson,
            User user,
            Authorizations authorizations
    ) {
        OpenLumifyVisibility openlumifyVisibility = visibilityTranslator.toVisibility(visibilityJson);
        ElementBuilder<Edge> edgeBuilder;
        if (edgeId == null) {
            edgeBuilder = graph.prepareEdge(outVertex, inVertex, predicateLabel, openlumifyVisibility.getVisibility());
        } else {
            edgeBuilder = graph.prepareEdge(
                    edgeId,
                    outVertex,
                    inVertex,
                    predicateLabel,
                    openlumifyVisibility.getVisibility()
            );
        }
        updateElementMetadataProperties(edgeBuilder, null, visibilityJson, user);

        boolean justificationAdded = addJustification(
                edgeBuilder,
                justificationText,
                openlumifyVisibility,
                visibilityJson,
                user
        );

        Edge edge = edgeBuilder.save(authorizations);

        if (justificationAdded) {
            termMentionRepository.removeSourceInfoEdgeFromEdge(edge, null, null, openlumifyVisibility, authorizations);
        } else if (sourceInfo != null) {
            OpenLumifyProperties.JUSTIFICATION.removeProperty(edgeBuilder, openlumifyVisibility.getVisibility());

            Vertex sourceDataVertex = graph.getVertex(sourceInfo.vertexId, authorizations);
            termMentionRepository.addSourceInfoEdgeToEdge(
                    edge,
                    edge.getId(),
                    TermMentionFor.EDGE,
                    null,
                    null,
                    null,
                    sourceInfo.snippet,
                    sourceInfo.textPropertyKey,
                    sourceInfo.textPropertyName,
                    sourceInfo.startOffset,
                    sourceInfo.endOffset,
                    sourceDataVertex,
                    openlumifyVisibility.getVisibility(),
                    authorizations
            );
        }

        return edge;
    }

    private void updateElementMetadataProperties(
            ElementBuilder elementBuilder,
            String conceptType,
            VisibilityJson visibilityJson,
            User user
    ) {
        Visibility defaultVisibility = visibilityTranslator.getDefaultVisibility();
        Date now = new Date();

        if (!Strings.isNullOrEmpty(conceptType)) {
            OpenLumifyProperties.CONCEPT_TYPE.setProperty(
                    elementBuilder,
                    conceptType,
                    defaultVisibility
            );
        }
        OpenLumifyProperties.VISIBILITY_JSON.setProperty(
                elementBuilder,
                visibilityJson,
                defaultVisibility
        );
        OpenLumifyProperties.MODIFIED_DATE.setProperty(elementBuilder, now, defaultVisibility);
        OpenLumifyProperties.MODIFIED_BY.setProperty(elementBuilder, user.getUserId(), defaultVisibility);
    }

    private boolean addJustification(
            ElementBuilder elementBuilder,
            String justificationText,
            OpenLumifyVisibility openlumifyVisibility,
            VisibilityJson visibilityJson,
            User user
    ) {
        Visibility visibility = openlumifyVisibility.getVisibility();
        if (justificationText != null) {
            Metadata metadata = new Metadata();
            Visibility metadataVisibility = visibilityTranslator.getDefaultVisibility();
            OpenLumifyProperties.MODIFIED_DATE_METADATA.setMetadata(metadata, new Date(), metadataVisibility);
            OpenLumifyProperties.MODIFIED_BY_METADATA.setMetadata(metadata, user.getUserId(), metadataVisibility);
            OpenLumifyProperties.VISIBILITY_JSON_METADATA.setMetadata(metadata, visibilityJson, metadataVisibility);

            PropertyJustificationMetadata value = new PropertyJustificationMetadata(justificationText);
            OpenLumifyProperties.JUSTIFICATION.setProperty(elementBuilder, value, metadata, visibility);
            return true;
        }
        return false;
    }

    public GraphUpdateContext beginGraphUpdate(Priority priority, User user, Authorizations authorizations) {
        return new MyGraphUpdateContext(
                graph,
                workQueueRepository,
                visibilityTranslator,
                priority,
                user,
                authorizations
        );
    }

    private static class MyGraphUpdateContext extends GraphUpdateContext {
        protected MyGraphUpdateContext(
                Graph graph,
                WorkQueueRepository workQueueRepository,
                VisibilityTranslator visibilityTranslator,
                Priority priority,
                User user,
                Authorizations authorizations
        ) {
            super(graph, workQueueRepository, visibilityTranslator, priority, user, authorizations);
        }
    }
}
