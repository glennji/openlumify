package org.openlumify.core.ingest.graphProperty;

import com.google.inject.Inject;
import org.vertexium.*;
import org.vertexium.mutation.ExistingElementMutation;
import org.vertexium.property.StreamingPropertyValue;
import org.openlumify.core.config.Configuration;
import org.openlumify.core.ingest.video.VideoTranscript;
import org.openlumify.core.model.graph.GraphRepository;
import org.openlumify.core.model.ontology.OntologyRepository;
import org.openlumify.core.model.properties.MediaOpenLumifyProperties;
import org.openlumify.core.model.properties.OpenLumifyProperties;
import org.openlumify.core.model.properties.types.OpenLumifyPropertyUpdate;
import org.openlumify.core.model.user.GraphAuthorizationRepository;
import org.openlumify.core.model.workQueue.Priority;
import org.openlumify.core.model.workQueue.WorkQueueRepository;
import org.openlumify.core.model.workspace.WorkspaceRepository;
import org.openlumify.core.security.VisibilityTranslator;
import org.openlumify.core.user.User;
import org.openlumify.core.util.RowKeyHelper;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

public abstract class GraphPropertyWorker {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(GraphPropertyWorker.class);
    private Graph graph;
    private VisibilityTranslator visibilityTranslator;
    private WorkQueueRepository workQueueRepository;
    private OntologyRepository ontologyRepository;
    private GraphAuthorizationRepository graphAuthorizationRepository;
    private GraphPropertyWorkerPrepareData workerPrepareData;
    private Configuration configuration;
    private WorkspaceRepository workspaceRepository;
    private GraphRepository graphRepository;

    public VerifyResults verify() {
        return new VerifyResults();
    }

    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        this.workerPrepareData = workerPrepareData;
    }

    protected void applyTermMentionFilters(Vertex outVertex, Iterable<Vertex> termMentions) {
        for (TermMentionFilter termMentionFilter : this.workerPrepareData.getTermMentionFilters()) {
            try {
                termMentionFilter.apply(outVertex, termMentions, this.workerPrepareData.getAuthorizations());
            } catch (Exception e) {
                LOGGER.error("Could not apply term mention filter", e);
            }
        }
        getGraph().flush();
    }

    protected void pushTextUpdated(GraphPropertyWorkData data) {
        if (data == null || data.getElement() == null) {
            return;
        }
        getWorkQueueRepository().pushTextUpdated(data.getElement().getId(), data.getPriority());
    }

    public abstract boolean isHandled(Element element, Property property);

    public boolean isDeleteHandled(Element element, Property property) {
        return false;
    }

    public boolean isHiddenHandled(Element element, Property property) {
        return false;
    }

    public boolean isUnhiddenHandled(Element element, Property property) {
        return false;
    }

    public abstract void execute(InputStream in, GraphPropertyWorkData data) throws Exception;

    public boolean isLocalFileRequired() {
        return false;
    }

    protected User getUser() {
        return this.workerPrepareData.getUser();
    }

    public Authorizations getAuthorizations() {
        return this.workerPrepareData.getAuthorizations();
    }

    @Inject
    public final void setGraph(Graph graph) {
        this.graph = graph;
    }

    protected Graph getGraph() {
        return graph;
    }

    @Inject
    public final void setWorkQueueRepository(WorkQueueRepository workQueueRepository) {
        this.workQueueRepository = workQueueRepository;
    }

    @Inject
    public final void setWorkspaceRepository(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    protected WorkspaceRepository getWorkspaceRepository() {
        return workspaceRepository;
    }

    protected WorkQueueRepository getWorkQueueRepository() {
        return workQueueRepository;
    }

    protected OntologyRepository getOntologyRepository() {
        return ontologyRepository;
    }

    @Inject
    public final void setOntologyRepository(OntologyRepository ontologyRepository) {
        this.ontologyRepository = ontologyRepository;
    }

    protected Configuration getConfiguration() {
        return configuration;
    }

    @Inject
    public final void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    protected VisibilityTranslator getVisibilityTranslator() {
        return visibilityTranslator;
    }

    @Inject
    public final void setVisibilityTranslator(VisibilityTranslator visibilityTranslator) {
        this.visibilityTranslator = visibilityTranslator;
    }

    @Inject
    public final void setGraphAuthorizationRepository(GraphAuthorizationRepository graphAuthorizationRepository) {
        this.graphAuthorizationRepository = graphAuthorizationRepository;
    }

    protected GraphAuthorizationRepository getGraphAuthorizationRepository() {
        return graphAuthorizationRepository;
    }

    public GraphRepository getGraphRepository() {
        return graphRepository;
    }

    @Inject
    public final void setGraphRepository(GraphRepository graphRepository) {
        this.graphRepository = graphRepository;
    }

    /**
     * Determines if this is a property that should be analyzed by text processing tools.
     */
    protected boolean isTextProperty(Property property) {
        if (property == null) {
            return false;
        }

        if (property.getName().equals(OpenLumifyProperties.RAW.getPropertyName())) {
            return false;
        }

        String mimeType = (String) property.getMetadata().getValue(OpenLumifyProperties.MIME_TYPE.getPropertyName());
        return !(mimeType == null || !mimeType.startsWith("text"));
    }

    protected static boolean isVertex(Element element) {
        if (!(element instanceof Vertex)) {
            return false;
        }
        return true;
    }

    protected static boolean isConceptType(Element element, String conceptType) {
        String elementConceptType = OpenLumifyProperties.CONCEPT_TYPE.getPropertyValue(element);
        if (elementConceptType == null) {
            return false;
        }

        if (!elementConceptType.equals(conceptType)) {
            return false;
        }

        return true;
    }

    protected void addVideoTranscriptAsTextPropertiesToMutation(ExistingElementMutation<Vertex> mutation, String propertyKey, VideoTranscript videoTranscript, Metadata metadata, Visibility visibility) {
        OpenLumifyProperties.MIME_TYPE_METADATA.setMetadata(metadata, "text/plain", getVisibilityTranslator().getDefaultVisibility());
        for (VideoTranscript.TimedText entry : videoTranscript.getEntries()) {
            String textPropertyKey = getVideoTranscriptTimedTextPropertyKey(propertyKey, entry);
            StreamingPropertyValue value = StreamingPropertyValue.create(new ByteArrayInputStream(entry.getText().getBytes()), String.class);
            OpenLumifyProperties.TEXT.addPropertyValue(mutation, textPropertyKey, value, metadata, visibility);
        }
    }

    protected void pushVideoTranscriptTextPropertiesOnWorkQueue(Element element, String propertyKey, VideoTranscript videoTranscript, Priority priority) {
        for (VideoTranscript.TimedText entry : videoTranscript.getEntries()) {
            String textPropertyKey = getVideoTranscriptTimedTextPropertyKey(propertyKey, entry);
            getWorkQueueRepository().pushGraphPropertyQueue(element, textPropertyKey, OpenLumifyProperties.TEXT.getPropertyName(), priority);
        }
    }

    private String getVideoTranscriptTimedTextPropertyKey(String propertyKey, VideoTranscript.TimedText entry) {
        String startTime = String.format("%08d", Math.max(0L, entry.getTime().getStart()));
        String endTime = String.format("%08d", Math.max(0L, entry.getTime().getEnd()));
        return propertyKey + RowKeyHelper.FIELD_SEPARATOR + MediaOpenLumifyProperties.VIDEO_FRAME.getPropertyName() + RowKeyHelper.FIELD_SEPARATOR + startTime + RowKeyHelper.FIELD_SEPARATOR + endTime;
    }

    protected void addVertexToWorkspaceIfNeeded(GraphPropertyWorkData data, Vertex vertex) {
        if (data.getWorkspaceId() == null) {
            return;
        }
        graph.flush();
        getWorkspaceRepository().updateEntityOnWorkspace(data.getWorkspaceId(), vertex.getId(), getUser());
    }

    protected void pushChangedPropertiesOnWorkQueue(GraphPropertyWorkData data, List<OpenLumifyPropertyUpdate> changedProperties) {
        getWorkQueueRepository().pushGraphOpenLumifyPropertyQueue(
                data.getElement(),
                changedProperties,
                data.getWorkspaceId(),
                data.getVisibilitySource(),
                data.getPriority()
        );
    }
}
