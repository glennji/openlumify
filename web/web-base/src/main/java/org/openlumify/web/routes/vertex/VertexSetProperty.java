package org.openlumify.web.routes.vertex;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.webster.ParameterizedHandler;
import org.openlumify.webster.annotations.Handle;
import org.openlumify.webster.annotations.Optional;
import org.openlumify.webster.annotations.Required;
import org.vertexium.*;
import org.vertexium.util.IterableUtils;
import org.openlumify.core.config.Configuration;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.model.graph.GraphRepository;
import org.openlumify.core.model.graph.VisibilityAndElementMutation;
import org.openlumify.core.model.ontology.OntologyProperty;
import org.openlumify.core.model.ontology.OntologyRepository;
import org.openlumify.core.model.workQueue.Priority;
import org.openlumify.core.model.workQueue.WorkQueueRepository;
import org.openlumify.core.model.workspace.WorkspaceHelper;
import org.openlumify.core.model.workspace.WorkspaceRepository;
import org.openlumify.core.security.ACLProvider;
import org.openlumify.core.security.VisibilityTranslator;
import org.openlumify.core.user.User;
import org.openlumify.core.util.*;
import org.openlumify.web.clientapi.model.ClientApiSourceInfo;
import org.openlumify.web.clientapi.model.ClientApiVertex;
import org.openlumify.web.clientapi.model.SandboxStatus;
import org.openlumify.web.clientapi.model.VisibilityJson;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;
import org.openlumify.web.parameterProviders.JustificationText;
import org.openlumify.web.routes.SetPropertyBase;
import org.openlumify.web.util.VisibilityValidator;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

@Singleton
public class VertexSetProperty extends SetPropertyBase implements ParameterizedHandler {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(VertexSetProperty.class);

    private final OntologyRepository ontologyRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkQueueRepository workQueueRepository;
    private final WorkspaceHelper workspaceHelper;
    private final GraphRepository graphRepository;
    private final ACLProvider aclProvider;
    private final boolean autoPublishComments;

    @Inject
    public VertexSetProperty(
            OntologyRepository ontologyRepository,
            Graph graph,
            VisibilityTranslator visibilityTranslator,
            WorkspaceRepository workspaceRepository,
            WorkQueueRepository workQueueRepository,
            WorkspaceHelper workspaceHelper,
            GraphRepository graphRepository,
            ACLProvider aclProvider,
            Configuration configuration
    ) {
        super(graph, visibilityTranslator);
        this.ontologyRepository = ontologyRepository;
        this.workspaceRepository = workspaceRepository;
        this.workQueueRepository = workQueueRepository;
        this.workspaceHelper = workspaceHelper;
        this.graphRepository = graphRepository;
        this.aclProvider = aclProvider;
        this.autoPublishComments = configuration.getBoolean(
                Configuration.COMMENTS_AUTO_PUBLISH,
                Configuration.DEFAULT_COMMENTS_AUTO_PUBLISH
        );
    }

    @Handle
    public ClientApiVertex handle(
            HttpServletRequest request,
            @Required(name = "graphVertexId") String graphVertexId,
            @Optional(name = "propertyKey") String propertyKey,
            @Required(name = "propertyName") String propertyName,
            @Optional(name = "value") String valueStr,
            @Optional(name = "values") String valuesStr,
            @Required(name = "visibilitySource") String visibilitySource,
            @Optional(name = "oldVisibilitySource") String oldVisibilitySource,
            @Optional(name = "sourceInfo") String sourceInfoString,
            @Optional(name = "metadata") String metadataString,
            @JustificationText String justificationText,
            @ActiveWorkspaceId String workspaceId,
            ResourceBundle resourceBundle,
            User user,
            Authorizations authorizations
    ) throws Exception {
        if (valueStr == null && valuesStr == null) {
            throw new OpenLumifyException("Parameter: 'value' or 'values' is required in the request");
        }

        VisibilityValidator.validate(graph, visibilityTranslator, resourceBundle, visibilitySource, user, authorizations);
        checkRoutePath("vertex", propertyName, request);

        boolean isComment = isCommentProperty(propertyName);
        boolean autoPublish = isComment && autoPublishComments;
        if (autoPublish) {
            workspaceId = null;
        }

        if (propertyKey == null) {
            propertyKey = createPropertyKey(propertyName, graph);
        }

        Metadata metadata = VertexiumMetadataUtil.metadataStringToMap(metadataString, visibilityTranslator.getDefaultVisibility());
        ClientApiSourceInfo sourceInfo = ClientApiSourceInfo.fromString(sourceInfoString);
        Vertex vertex = graph.getVertex(graphVertexId, authorizations);

        aclProvider.checkCanAddOrUpdateProperty(vertex, propertyKey, propertyName, user, workspaceId);

        List<SavePropertyResults> savePropertyResults = saveProperty(
                vertex,
                propertyKey,
                propertyName,
                valueStr,
                valuesStr,
                justificationText,
                oldVisibilitySource,
                visibilitySource,
                metadata,
                sourceInfo,
                user,
                workspaceId,
                authorizations
        );
        graph.flush();

        if (!autoPublish) {
            // add the vertex to the workspace so that the changes show up in the diff panel
            workspaceRepository.updateEntityOnWorkspace(workspaceId, vertex.getId(), user);
        }

        for (SavePropertyResults savePropertyResult : savePropertyResults) {
            workQueueRepository.pushGraphPropertyQueue(
                    vertex,
                    savePropertyResult.getPropertyKey(),
                    savePropertyResult.getPropertyName(),
                    workspaceId,
                    visibilitySource,
                    Priority.HIGH
            );
        }

        if (sourceInfo != null) {
            workQueueRepository.pushTextUpdated(sourceInfo.vertexId);
        }

        return (ClientApiVertex) ClientApiConverter.toClientApi(vertex, workspaceId, authorizations);
    }

    private List<SavePropertyResults> saveProperty(
            Vertex vertex,
            String propertyKey,
            String propertyName,
            String valueStr,
            String valuesStr,
            String justificationText,
            String oldVisibilitySource,
            String visibilitySource,
            Metadata metadata,
            ClientApiSourceInfo sourceInfo,
            User user,
            String workspaceId,
            Authorizations authorizations
    ) {
        Object value;
        if (isCommentProperty(propertyName)) {
            value = valueStr;
        } else {
            OntologyProperty property = ontologyRepository.getRequiredPropertyByIRI(propertyName, workspaceId);
            if (property.hasDependentPropertyIris()) {
                return saveDependentProperties(valuesStr,
                        property,
                        oldVisibilitySource,
                        vertex,
                        propertyKey,
                        justificationText,
                        visibilitySource,
                        metadata,
                        sourceInfo,
                        workspaceId,
                        user,
                        authorizations);
            } else {
                if (valueStr == null && valuesStr == null) {
                    throw new OpenLumifyException("properties without dependent properties must have a value");
                }
                try {
                    value = (valuesStr == null ? property.convertString(valueStr) : property.convertString(valuesStr));
                } catch (Exception ex) {
                    LOGGER.warn(String.format("Validation error propertyName: %s, valueStr: %s", propertyName, valueStr), ex);
                    throw new OpenLumifyException(ex.getMessage(), ex);
                }
            }
        }

        VisibilityAndElementMutation<Vertex> setPropertyResult = graphRepository.setProperty(
                vertex,
                propertyName,
                propertyKey,
                value,
                metadata,
                oldVisibilitySource,
                visibilitySource,
                workspaceId,
                justificationText,
                sourceInfo,
                user,
                authorizations
        );
        Vertex save = setPropertyResult.elementMutation.save(authorizations);
        return Lists.newArrayList(new SavePropertyResults(save, propertyKey, propertyName));
    }

    private List<SavePropertyResults> saveDependentProperties(String valuesStr,
                                                              OntologyProperty property,
                                                              String oldVisibilitySource,
                                                              Vertex vertex,
                                                              String propertyKey,
                                                              String justificationText,
                                                              String visibilitySource,
                                                              Metadata metadata,
                                                              ClientApiSourceInfo sourceInfo,
                                                              String workspaceId,
                                                              User user,
                                                              Authorizations authorizations) {
        if (valuesStr == null || valuesStr.length() == 0) {
            throw new OpenLumifyException("ValuesStr must contain at least one property value for saving dependent properties");
        }
        Map<String, String> values;
        try {
            ObjectMapper mapper = new ObjectMapper();
            values = mapper.readValue(valuesStr, new TypeReference<Map<String, String>>() {
            });
        } catch (IOException e) {
            throw new OpenLumifyException("Unable to parse values", e);
        }

        List<SavePropertyResults> results = new ArrayList<>();
        for (String dependentPropertyIri : property.getDependentPropertyIris()) {
            if (values.get(dependentPropertyIri) == null) {
                VisibilityJson oldVisibilityJson = new VisibilityJson(oldVisibilitySource);
                oldVisibilityJson.addWorkspace(workspaceId);
                Visibility oldVisibility = visibilityTranslator.toVisibility(oldVisibilityJson).getVisibility();

                Property oldProperty = vertex.getProperty(propertyKey, dependentPropertyIri, oldVisibility);

                if (oldProperty != null) {
                    List<Property> properties = IterableUtils.toList(vertex.getProperties());
                    SandboxStatus[] sandboxStatuses = SandboxStatusUtil.getPropertySandboxStatuses(properties, workspaceId);
                    boolean isPropertyPublic = sandboxStatuses[properties.indexOf(oldProperty)] == SandboxStatus.PUBLIC;

                    workspaceHelper.deleteProperty(
                            vertex,
                            oldProperty,
                            isPropertyPublic,
                            workspaceId,
                            Priority.HIGH,
                            authorizations
                    );
                }
            } else {
                results.addAll(saveProperty(
                        vertex,
                        propertyKey,
                        dependentPropertyIri,
                        values.get(dependentPropertyIri),
                        null,
                        justificationText,
                        oldVisibilitySource,
                        visibilitySource,
                        metadata,
                        sourceInfo,
                        user,
                        workspaceId,
                        authorizations
                ));
            }
        }
        return results;
    }

    private static class SavePropertyResults {
        private final Vertex vertex;
        private final String propertyKey;
        private final String propertyName;

        public SavePropertyResults(Vertex vertex, String propertyKey, String propertyName) {
            this.vertex = vertex;
            this.propertyKey = propertyKey;
            this.propertyName = propertyName;
        }

        public Vertex getVertex() {
            return vertex;
        }

        public String getPropertyKey() {
            return propertyKey;
        }

        public String getPropertyName() {
            return propertyName;
        }
    }
}
