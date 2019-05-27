package org.openlumify.web.structuredingest.core.util;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import org.vertexium.*;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.model.graph.GraphRepository;
import org.openlumify.core.model.properties.OpenLumifyProperties;
import org.openlumify.core.model.properties.types.PropertyMetadata;
import org.openlumify.core.model.properties.types.SingleValueOpenLumifyProperty;
import org.openlumify.core.model.user.PrivilegeRepository;
import org.openlumify.core.model.workQueue.Priority;
import org.openlumify.core.model.workspace.Workspace;
import org.openlumify.core.model.workspace.WorkspaceHelper;
import org.openlumify.core.model.workspace.WorkspaceRepository;
import org.openlumify.core.security.OpenLumifyVisibility;
import org.openlumify.core.security.VisibilityTranslator;
import org.openlumify.core.user.User;
import org.openlumify.core.util.SandboxStatusUtil;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;
import org.openlumify.web.clientapi.model.Privilege;
import org.openlumify.web.clientapi.model.SandboxStatus;
import org.openlumify.web.clientapi.model.VisibilityJson;
import org.openlumify.web.structuredingest.core.StructuredIngestOntology;
import org.openlumify.web.structuredingest.core.model.ClientApiIngestPreview;
import org.openlumify.web.structuredingest.core.model.ClientApiParseErrors;
import org.openlumify.web.structuredingest.core.util.mapping.EdgeMapping;
import org.openlumify.web.structuredingest.core.util.mapping.ParseMapping;
import org.openlumify.web.structuredingest.core.util.mapping.PropertyMapping;
import org.openlumify.web.structuredingest.core.util.mapping.VertexMapping;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.openlumify.core.model.properties.OpenLumifyProperties.VISIBILITY_JSON_METADATA;

public class GraphBuilderParserHandler extends BaseStructuredFileParserHandler {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(GraphBuilderParserHandler.class);
    public static final Long MAX_DRY_RUN_ROWS = 50000L;
    private static final String MULTI_KEY = "SFIMPORT:";
    private static final String SKIPPED_VERTEX_ID = "SKIPPED_VERTEX";

    private final Graph graph;
    private final User user;
    private final VisibilityTranslator visibilityTranslator;
    private final PrivilegeRepository privilegeRepository;
    private final Authorizations authorizations;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceHelper workspaceHelper;
    private final Workspace workspace;
    private final Vertex structuredFileVertex;
    private final PropertyMetadata propertyMetadata;
    private final Visibility visibility;
    private final ParseMapping parseMapping;
    private final ProgressReporter progressReporter;
    private final Authorizations openlumifyUserAuths;

    private VisibilityJson visibilityJson;
    private boolean publish;
    private int sheetNumber = -1;
    public int maxParseErrors = 10;
    public boolean dryRun = true;
    public ClientApiParseErrors parseErrors = new ClientApiParseErrors();
    public ClientApiIngestPreview clientApiIngestPreview;
    public List<String> createdVertexIds;
    public List<String> createdEdgeIds;

    public GraphBuilderParserHandler(
            Graph graph,
            User user,
            VisibilityTranslator visibilityTranslator,
            PrivilegeRepository privilegeRepository,
            Authorizations authorizations,
            WorkspaceRepository workspaceRepository,
            WorkspaceHelper workspaceHelper,
            String workspaceId,
            boolean publish,
            Vertex structuredFileVertex,
            ParseMapping parseMapping,
            ProgressReporter progressReporter
    ) {
        this.graph = graph;
        this.user = user;
        this.visibilityTranslator = visibilityTranslator;
        this.privilegeRepository = privilegeRepository;
        this.authorizations = authorizations;
        this.workspaceRepository = workspaceRepository;
        this.workspaceHelper = workspaceHelper;
        this.workspace = workspaceRepository.findById(workspaceId, user);
        this.structuredFileVertex = structuredFileVertex;
        this.parseMapping = parseMapping;
        this.progressReporter = progressReporter;
        this.publish = publish;

        openlumifyUserAuths = graph.createAuthorizations(OpenLumifyVisibility.SUPER_USER_VISIBILITY_STRING);

        if (workspace == null) {
            throw new OpenLumifyException("Unable to find vertex with ID: " + workspaceId);
        }

        clientApiIngestPreview = new ClientApiIngestPreview();
        createdVertexIds = Lists.newArrayList();
        createdEdgeIds = Lists.newArrayList();
        visibilityJson = new VisibilityJson(visibilityTranslator.getDefaultVisibility().getVisibilityString());

        if (this.publish) {
            if (!privilegeRepository.hasPrivilege(user, Privilege.PUBLISH)) {
                this.publish = false;
            }
        }

        if (!this.publish) {
            visibilityJson.addWorkspace(workspaceId);
        }
        visibility = visibilityTranslator.toVisibility(visibilityJson).getVisibility();

        propertyMetadata = new PropertyMetadata(
                new Date(),
                user,
                GraphRepository.SET_PROPERTY_CONFIDENCE,
                visibilityJson,
                visibilityTranslator.getDefaultVisibility()
        );
    }

    public void reset() {
        parseErrors.errors.clear();
        sheetNumber = -1;
        clientApiIngestPreview = new ClientApiIngestPreview();
        createdVertexIds.clear();
        createdEdgeIds.clear();
    }

    public boolean hasErrors() {
        return !parseErrors.errors.isEmpty();
    }

    @Override
    public void newSheet(String name) {
        // Right now, it will ingest all of the columns in the first sheet since that's
        // what the interface shows. In the future, if they can select a different sheet
        // this code will need to be updated.
        sheetNumber++;
    }

    @Override
    public boolean addRow(Map<String, Object> row, long rowNum) {
        Long rowCount = rowNum + 1;
        if (dryRun && rowCount > MAX_DRY_RUN_ROWS) {
            clientApiIngestPreview.didTruncate = true;
            return false;
        }
        clientApiIngestPreview.processedRows = rowCount;

        Visibility defaultVisibility = visibilityTranslator.getDefaultVisibility();

        // Since we only handle the first sheet currently, bail if this isn't it.
        if (sheetNumber != 0) {
            return false;
        }

        try {
            List<String> newVertexIds = new ArrayList<>();
            List<VertexBuilder> vertexBuilders = new ArrayList<>();
            List<String> workspaceUpdates = new ArrayList<>();
            long vertexNum = 0;
            for (VertexMapping vertexMapping : parseMapping.vertexMappings) {
                VertexBuilder vertexBuilder = createVertex(vertexMapping, row, rowNum, vertexNum);
                if (vertexBuilder != null) {
                    boolean alreadyCreated = createdVertexIds.contains(vertexBuilder.getVertexId());
                    vertexBuilders.add(vertexBuilder);
                    newVertexIds.add(vertexBuilder.getVertexId());
                    createdVertexIds.add(vertexBuilder.getVertexId());
                    workspaceUpdates.add(vertexBuilder.getVertexId());
                    if (!alreadyCreated) {
                        incrementConcept(vertexMapping, !graph.doesVertexExist(vertexBuilder.getVertexId(), authorizations));
                    }
                } else {
                    newVertexIds.add(SKIPPED_VERTEX_ID);
                }
                vertexNum++;
            }

            List<EdgeBuilderByVertexId> edgeBuilders = new ArrayList<>();
            for (EdgeMapping edgeMapping : parseMapping.edgeMappings) {
                EdgeBuilderByVertexId edgeBuilder = createEdge(edgeMapping, newVertexIds);
                if (edgeBuilder != null) {
                    boolean alreadyCreated = createdEdgeIds.contains(edgeBuilder.getEdgeId());
                    createdEdgeIds.add(edgeBuilder.getEdgeId());
                    edgeBuilders.add(edgeBuilder);
                    if (!alreadyCreated) {
                        incrementEdges(edgeMapping, !graph.doesEdgeExist(edgeBuilder.getEdgeId(), authorizations));
                    }
                }
            }

            if (!dryRun) {
                HashFunction hash = Hashing.sha1();
                for (VertexBuilder vertexBuilder : vertexBuilders) {
                    Vertex newVertex = vertexBuilder.save(authorizations);
                    EdgeBuilder hasSourceEdgeBuilder = graph.prepareEdge(
                            hash.newHasher()
                                    .putString(newVertex.getId())
                                    .putString(structuredFileVertex.getId())
                                    .hash()
                                    .toString(),
                            newVertex,
                            structuredFileVertex,
                            StructuredIngestOntology.ELEMENT_HAS_SOURCE_IRI,
                            visibility
                    );
                    OpenLumifyProperties.VISIBILITY_JSON.setProperty(hasSourceEdgeBuilder, visibilityJson, defaultVisibility);
                    OpenLumifyProperties.MODIFIED_BY.setProperty(hasSourceEdgeBuilder, user.getUserId(), defaultVisibility);
                    OpenLumifyProperties.MODIFIED_DATE.setProperty(hasSourceEdgeBuilder, new Date(), defaultVisibility);
                    hasSourceEdgeBuilder.save(authorizations);
                }

                for (EdgeBuilderByVertexId edgeBuilder : edgeBuilders) {
                    edgeBuilder.save(authorizations);
                }

                graph.flush();

                if (!this.publish && workspaceUpdates.size() > 0) {
                    workspaceRepository.updateEntitiesOnWorkspace(workspace, workspaceUpdates, user);
                }
            }
        } catch (SkipRowException sre) {
            // Skip the row and keep going
        }

        if (progressReporter != null) {
            progressReporter.finishedRow(rowNum, getTotalRows());
        }

        return !dryRun || maxParseErrors <= 0 || parseErrors.errors.size() < maxParseErrors;
    }

    private void incrementConcept(VertexMapping vertexMapping, boolean isNew) {
        for (PropertyMapping mapping : vertexMapping.propertyMappings) {
            if (OpenLumifyProperties.CONCEPT_TYPE.getPropertyName().equals(mapping.name)) {
                clientApiIngestPreview.incrementVertices(mapping.value, isNew);
            }
        }
    }

    private void incrementEdges(EdgeMapping mapping, boolean isNew) {
        clientApiIngestPreview.incrementEdges(mapping.label, isNew);
    }

    public boolean cleanUpExistingImport() {
        Iterable<Vertex> vertices = structuredFileVertex.getVertices(
                Direction.IN,
                StructuredIngestOntology.ELEMENT_HAS_SOURCE_IRI,
                authorizations
        );

        for (Vertex vertex : vertices) {
            SandboxStatus sandboxStatus = SandboxStatusUtil.getSandboxStatus(vertex, workspace.getWorkspaceId());
            if (sandboxStatus != SandboxStatus.PUBLIC) {
                workspaceHelper.deleteVertex(
                        vertex,
                        workspace.getWorkspaceId(),
                        false,
                        Priority.HIGH,
                        authorizations,
                        user
                );
            }
        }

        return true;
    }

    private EdgeBuilderByVertexId createEdge(EdgeMapping edgeMapping, List<String> newVertexIds) {
        Visibility defaultVisibility = visibilityTranslator.getDefaultVisibility();
        String inVertexId = newVertexIds.get(edgeMapping.inVertexIndex);
        String outVertexId = newVertexIds.get(edgeMapping.outVertexIndex);

        if (inVertexId.equals(SKIPPED_VERTEX_ID) || outVertexId.equals(SKIPPED_VERTEX_ID)) {
            // TODO: handle edge errors properly?
            return null;
        }

        VisibilityJson edgeVisibilityJson = visibilityJson;
        Visibility edgeVisibility = visibility;
        if (edgeMapping.visibilityJson != null) {
            edgeVisibilityJson = edgeMapping.visibilityJson;
            edgeVisibility = edgeMapping.visibility;
        }

        EdgeBuilderByVertexId m = graph.prepareEdge(outVertexId, inVertexId, edgeMapping.label, edgeVisibility);
        OpenLumifyProperties.VISIBILITY_JSON.setProperty(m, edgeVisibilityJson, edgeVisibility);
        OpenLumifyProperties.MODIFIED_DATE.setProperty(m, propertyMetadata.getModifiedDate(), edgeVisibility);
        OpenLumifyProperties.MODIFIED_BY.setProperty(m, propertyMetadata.getModifiedBy().getUserId(), edgeVisibility);
        return m;
    }

    private VertexBuilder createVertex(VertexMapping vertexMapping, Map<String, Object> row, long rowNum, long vertexNum) {
        VisibilityJson vertexVisibilityJson = visibilityJson;
        Visibility vertexVisibility = visibility;
        if (vertexMapping.visibilityJson != null) {
            vertexVisibilityJson = vertexMapping.visibilityJson;
            vertexVisibility = vertexMapping.visibility;
        }

        String vertexId = generateVertexId(vertexMapping, row, rowNum, vertexNum);

        VertexBuilder m = vertexId == null ? graph.prepareVertex(vertexVisibility) : graph.prepareVertex(vertexId, vertexVisibility);
        setPropertyValue(OpenLumifyProperties.VISIBILITY_JSON, m, vertexVisibilityJson, vertexVisibility);

        for (PropertyMapping propertyMapping : vertexMapping.propertyMappings) {

            if (OpenLumifyProperties.CONCEPT_TYPE.getPropertyName().equals(propertyMapping.name)) {
                setPropertyValue(OpenLumifyProperties.CONCEPT_TYPE, m, propertyMapping.value, vertexVisibility);
                setPropertyValue(OpenLumifyProperties.MODIFIED_DATE, m, propertyMetadata.getModifiedDate(), vertexVisibility);
                setPropertyValue(OpenLumifyProperties.MODIFIED_BY, m, propertyMetadata.getModifiedBy().getUserId(), vertexVisibility);
            } else {
                Metadata metadata = propertyMetadata.createMetadata();
                try {
                    OpenLumifyProperties.SOURCE_FILE_OFFSET_METADATA.setMetadata(metadata, Long.valueOf(rowNum), vertexVisibility);
                    setPropertyValue(m, row, propertyMapping, vertexVisibility, metadata);
                } catch (Exception e) {
                    LOGGER.error("Error parsing property.", e);

                    ClientApiParseErrors.ParseError pe = new ClientApiParseErrors.ParseError();
                    pe.rawPropertyValue = propertyMapping.extractRawValue(row);
                    pe.propertyMapping = propertyMapping;
                    pe.message = e.getMessage();
                    pe.sheetIndex = sheetNumber;
                    pe.rowIndex = rowNum;

                    if (!dryRun) {
                        if (propertyMapping.errorHandlingStrategy == PropertyMapping.ErrorHandlingStrategy.SKIP_ROW) {
                            throw new SkipRowException("Error parsing property.", e);
                        } else if (propertyMapping.errorHandlingStrategy == PropertyMapping.ErrorHandlingStrategy.SKIP_VERTEX) {
                            return null;
                        } else if (propertyMapping.errorHandlingStrategy == PropertyMapping.ErrorHandlingStrategy.SET_CELL_ERROR_PROPERTY) {
                            String multiKey = sheetNumber + "_" + rowNum;
                            StructuredIngestOntology.ERROR_MESSAGE_PROPERTY.addPropertyValue(
                                    m,
                                    multiKey,
                                    pe.message,
                                    metadata,
                                    vertexVisibility
                            );
                            StructuredIngestOntology.RAW_CELL_VALUE_PROPERTY.addPropertyValue(
                                    m,
                                    multiKey,
                                    pe.rawPropertyValue.toString(),
                                    metadata,
                                    vertexVisibility
                            );
                            StructuredIngestOntology.TARGET_PROPERTY.addPropertyValue(
                                    m,
                                    multiKey,
                                    pe.propertyMapping.name,
                                    metadata,
                                    vertexVisibility
                            );
                            StructuredIngestOntology.SHEET_PROPERTY.addPropertyValue(
                                    m,
                                    multiKey,
                                    String.valueOf(sheetNumber),
                                    metadata,
                                    vertexVisibility
                            );
                            StructuredIngestOntology.ROW_PROPERTY.addPropertyValue(
                                    m,
                                    multiKey,
                                    String.valueOf(rowNum),
                                    metadata,
                                    vertexVisibility
                            );
                        } else if (propertyMapping.errorHandlingStrategy != PropertyMapping.ErrorHandlingStrategy.SKIP_CELL) {
                            throw new OpenLumifyException("Unhandled mapping error. Please provide a strategy.");
                        }
                    } else if (propertyMapping.errorHandlingStrategy == null) {
                        parseErrors.errors.add(pe);
                    }
                }
            }
        }

        return m;
    }

    private String generateVertexId(VertexMapping vertexMapping, Map<String, Object> row, long rowNum, long vertexNum) {
        List<String> identifierParts = new ArrayList<>();

        // Find any mappings that designate identifier columns
        for (String key : row.keySet()) {
            for (PropertyMapping mapping : vertexMapping.propertyMappings) {
                if (mapping.key.equals(key) && mapping.identifier) {
                    Object val = row.get(key);
                    if (val != null && !val.toString().isEmpty()) {
                        identifierParts.add(key);
                    }
                }

            }
        }

        HashFunction sha1 = Hashing.sha1();
        Hasher hasher = sha1.newHasher();

        if (identifierParts.isEmpty()) {
            // By default just allow the same file to ingest without creating new entities
            hasher
                .putString(structuredFileVertex.getId()).putString("|")
                .putLong(rowNum).putString("|")
                .putLong(vertexNum);
        } else {
            // Hash all the identifier values and the concept. Use delimiter to minimize collisions
            identifierParts
                    .stream()
                    .sorted(String::compareToIgnoreCase)
                    .forEach(s -> {
                        hasher.putString(prepareValueForHash(row.get(s)), Charsets.UTF_8).putString("|");
                    });

            for (PropertyMapping mapping : vertexMapping.propertyMappings) {
                if (OpenLumifyProperties.CONCEPT_TYPE.getPropertyName().equals(mapping.name)) {
                    hasher.putString(mapping.value);
                }
            }
        }


        HashCode hash = hasher.hash();
        String vertexId = hash.toString();

        // We might need to also hash the workspace if this vertex exists in the system but not visible to user.
        if (shouldAddWorkspaceToId(vertexId)) {
            vertexId = sha1.newHasher()
                    .putString(vertexId)
                    .putString(workspace.getWorkspaceId())
                    .hash()
                    .toString();
        }

        return vertexId;
    }

    private String prepareValueForHash(Object obj) {
        return String.valueOf(obj).trim().toLowerCase();
    }

    /**
     * If the user is creating an entity that is unpublished in different sandbox, this user won't be able to access
     * it since prepareVertex with same id won't change the visibility.
     */
    private boolean shouldAddWorkspaceToId(String vertexId) {
        boolean vertexExistsForUser = graph.doesVertexExist(vertexId, authorizations);
        if (!vertexExistsForUser) {
            boolean vertexExistsInSystem = graph.doesVertexExist(vertexId, openlumifyUserAuths);
            if (vertexExistsInSystem) {
                return true;
            }
        }
        return false;
    }

    private void setPropertyValue(SingleValueOpenLumifyProperty property, VertexBuilder m, Object value, Visibility vertexVisibility) {
        Metadata metadata = propertyMetadata.createMetadata();
        property.setProperty(m, value, metadata, vertexVisibility);
    }

    private void setPropertyValue(
            VertexBuilder m, Map<String, Object> row, PropertyMapping propertyMapping, Visibility vertexVisibility,
            Metadata metadata
    ) throws Exception {
        Visibility propertyVisibility = vertexVisibility;
        if (propertyMapping.visibility != null) {
            propertyVisibility = propertyMapping.visibility;
            VISIBILITY_JSON_METADATA.setMetadata(
                    metadata, propertyMapping.visibilityJson, visibilityTranslator.getDefaultVisibility());
        }

        Object propertyValue = propertyMapping.decodeValue(row);
        if (propertyValue != null) {
            Hasher hasher = Hashing.sha1().newHasher();
            hasher.putString(prepareValueForHash(propertyValue), Charsets.UTF_8);
            String keySuffix = hasher.hash().toString();
            m.addPropertyValue(MULTI_KEY + keySuffix, propertyMapping.name, propertyValue, metadata, propertyVisibility);
        }
    }
}
