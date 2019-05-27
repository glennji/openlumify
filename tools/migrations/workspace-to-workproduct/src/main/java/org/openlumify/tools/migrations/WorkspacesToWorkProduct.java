package org.openlumify.tools.migrations;

import com.beust.jcommander.Parameters;
import org.json.JSONObject;
import org.vertexium.*;
import org.vertexium.query.GraphQuery;
import org.vertexium.query.QueryResultsIterable;
import org.openlumify.core.cmdline.CommandLineTool;
import org.openlumify.core.model.properties.OpenLumifyProperties;
import org.openlumify.core.model.workspace.WorkspaceProperties;
import org.openlumify.core.model.workspace.WorkspaceRepository;
import org.openlumify.core.security.OpenLumifyVisibility;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;

import static org.openlumify.core.model.workspace.WorkspaceProperties.WORKSPACE_TO_PRODUCT_RELATIONSHIP_IRI;
import static org.openlumify.core.util.StreamUtil.stream;

@Parameters(commandDescription = "Migrate workspaces to work products")
public class WorkspacesToWorkProduct extends MigrationBase {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(WorkspacesToWorkProduct.class);

    private static final String GRAPH_KIND = "org.openlumify.web.product.graph.GraphWorkProduct";
    private static final String EDGE_POSITION = "http://openlumify.org/workspace/product/graph#entityPosition";
    private static final String GRAPH_VISIBLE = "http://openlumify.org/workspace#toEntity/visible";
    private static final String GRAPH_POSITION_X = "http://openlumify.org/workspace#toEntity/graphPositionX";
    private static final String GRAPH_POSITION_Y = "http://openlumify.org/workspace#toEntity/graphPositionY";
    private static final String PRODUCT_TO_ENTITY_RELATIONSHIP_IRI = "http://openlumify.org/workspace/product#toEntity";
    private static final Integer VISALLO_GRAPH_VERSION_NEEDS_MIGRATION = 1;
    private static final Integer VISALLO_GRAPH_VERSION_BUMP = 2;

    public static void main(String[] args) throws Exception {
        CommandLineTool.main(new WorkspacesToWorkProduct(), args, false);
    }

    @Override
    public Integer getNeededGraphVersion() {
        return VISALLO_GRAPH_VERSION_NEEDS_MIGRATION;
    }

    @Override
    public Integer getFinalGraphVersion() {
        return VISALLO_GRAPH_VERSION_BUMP;
    }

    @Override
    protected boolean migrate(Graph graph) {
        Authorizations authorizations = graph.createAuthorizations(OpenLumifyVisibility.SUPER_USER_VISIBILITY_STRING);
        GraphQuery query = graph.query(authorizations);
        QueryResultsIterable<Vertex> workspaceVertices = query.has(OpenLumifyProperties.CONCEPT_TYPE.getPropertyName(), WorkspaceProperties.WORKSPACE_CONCEPT_IRI).vertices();

        stream(workspaceVertices)
                .forEach(workspace -> {
                    LOGGER.info("Found a workspace: %s", workspace.getId());

                    VertexBuilder productVertex = graph.prepareVertex(workspace.getVisibility());
                    OpenLumifyProperties.CONCEPT_TYPE.setProperty(
                            productVertex,
                            WorkspaceProperties.PRODUCT_CONCEPT_IRI,
                            workspace.getVisibility()
                    );
                    WorkspaceProperties.TITLE.setProperty(productVertex, "Migrated", workspace.getVisibility());
                    WorkspaceProperties.PRODUCT_KIND.setProperty(productVertex, GRAPH_KIND, workspace.getVisibility());
                    String productId = productVertex.getVertexId();
                    productVertex.save(authorizations);
                    EdgeBuilderByVertexId workspaceToProductEdge = graph.prepareEdge(workspace.getId() + "_hasProduct_" + productId, workspace.getId(), productId,
                            WORKSPACE_TO_PRODUCT_RELATIONSHIP_IRI, workspace.getVisibility());

                    workspaceToProductEdge.save(authorizations);
                    LOGGER.info("Creating product: %s", productId);


                    stream(workspace.getEdgeInfos(Direction.OUT, authorizations))
                            .forEach(edgeInfo -> {
                                if (edgeInfo.getLabel().equals(WorkspaceRepository.WORKSPACE_TO_ENTITY_RELATIONSHIP_IRI)) {

                                    Edge edge = getGraph().getEdge(edgeInfo.getEdgeId(), authorizations);
                                    if (edge != null && edge.getPropertyValue(GRAPH_VISIBLE).equals(true)) {
                                        LOGGER.info("\tCreating entity: %s", edgeInfo.getVertexId());

                                        String edgeId = productId + "_hasVertex_" + edgeInfo.getVertexId();
                                        EdgeBuilderByVertexId productToEntityEdge = graph.prepareEdge(
                                                edgeId,
                                                productId,
                                                edgeInfo.getVertexId(),
                                                PRODUCT_TO_ENTITY_RELATIONSHIP_IRI,
                                                workspace.getVisibility()
                                        );
                                        JSONObject position = new JSONObject();
                                        position.put("x", positionForProperty(edge, GRAPH_POSITION_X));
                                        position.put("y", positionForProperty(edge, GRAPH_POSITION_Y));
                                        productToEntityEdge.setProperty(EDGE_POSITION, position.toString(), workspace.getVisibility());
                                        productToEntityEdge.save(authorizations);
                                    }
                                }
                            });
                });
        return true;
    }

    private int positionForProperty(Edge edge, String name) {
        Integer val = (Integer) edge.getPropertyValue(name);
        if (val == null) return 0;
        return val;
    }
}

