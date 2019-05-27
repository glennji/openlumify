package org.openlumify.graphCheck.rules;

import org.vertexium.Edge;
import org.vertexium.Element;
import org.vertexium.Vertex;
import org.openlumify.core.model.properties.OpenLumifyProperties;
import org.openlumify.graphCheck.DefaultGraphCheckRule;
import org.openlumify.graphCheck.GraphCheckContext;

public class HasConceptTypeGraphCheckRule extends DefaultGraphCheckRule {
    @Override
    public void visitVertex(GraphCheckContext ctx, Vertex vertex) {
        hasConceptType(ctx, vertex);
    }

    @Override
    public void visitEdge(GraphCheckContext ctx, Edge edge) {
        hasConceptType(ctx, edge);
    }

    private void hasConceptType (GraphCheckContext ctx, Element element) {
        if (!OpenLumifyProperties.CONCEPT_TYPE.hasConceptType(element)) {
            ctx.reportError(this, element, "Missing \"%s\"", OpenLumifyProperties.CONCEPT_TYPE.getPropertyName());
        }
    }
}
