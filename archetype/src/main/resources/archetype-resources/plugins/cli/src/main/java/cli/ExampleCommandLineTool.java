#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.cli;

import com.beust.jcommander.Parameters;
import com.google.inject.Inject;
import ${package}.worker.OntologyConstants;
import org.vertexium.EdgeBuilderByVertexId;
import org.vertexium.Visibility;
import org.openlumify.core.cmdline.CommandLineTool;
import org.openlumify.core.model.graph.GraphRepository;
import org.openlumify.core.model.graph.GraphUpdateContext;
import org.openlumify.core.model.properties.types.PropertyMetadata;
import org.openlumify.core.model.workQueue.Priority;
import org.openlumify.web.clientapi.model.VisibilityJson;

@Parameters(commandDescription = "Example Command Line")
public class ExampleCommandLineTool extends CommandLineTool {
    private GraphRepository graphRepository;

    public static void main(String[] args) throws Exception {
        CommandLineTool.main(new ExampleCommandLineTool(), args);
    }

    @Override
    protected int run() throws Exception {
        VisibilityJson visibilityJson = new VisibilityJson("");
        Visibility visibility = getVisibilityTranslator().toVisibility(visibilityJson).getVisibility();
        PropertyMetadata propertyMetadata = new PropertyMetadata(getUser(), visibilityJson, visibility);

        // begin a graph update
        try (GraphUpdateContext ctx = graphRepository.beginGraphUpdate(Priority.NORMAL, getUser(), getAuthorizations())) {
            // create a vertex
            ctx.getOrCreateVertexAndUpdate("v1", visibility, elemCtx -> {
                if (elemCtx.isNewElement()) {
                    elemCtx.setConceptType(OntologyConstants.PERSON_CONCEPT_TYPE);
                    elemCtx.updateBuiltInProperties(propertyMetadata);
                }
                OntologyConstants.PERSON_FULL_NAME_PROPERTY.updateProperty(elemCtx, "John Doe", propertyMetadata);
            });

            // create a vertex
            ctx.getOrCreateVertexAndUpdate("v2", visibility, elemCtx -> {
                if (elemCtx.isNewElement()) {
                    elemCtx.setConceptType(OntologyConstants.PERSON_CONCEPT_TYPE);
                    elemCtx.updateBuiltInProperties(propertyMetadata);
                }
                OntologyConstants.PERSON_FULL_NAME_PROPERTY.updateProperty(elemCtx, "Jane Doe", propertyMetadata);
            });

            // create an edge
            if (!getGraph().doesEdgeExist("v1-to-v2", getAuthorizations())) {
                EdgeBuilderByVertexId e = getGraph().prepareEdge("v1-to-v2", "v1", "v2", OntologyConstants.KNOWS_EDGE_LABEL, visibility);
                ctx.update(e, elemCtx -> {
                    elemCtx.updateBuiltInProperties(propertyMetadata);
                });
            }
        }

        return 0;
    }

    @Inject
    public void setGraphRepository(GraphRepository graphRepository) {
        this.graphRepository = graphRepository;
    }
}
