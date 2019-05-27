package org.openlumify.web.product.graph;


import org.openlumify.core.model.properties.types.GraphPositionSingleValueOpenLumifyProperty;
import org.openlumify.core.model.properties.types.StringListSingleValueOpenLumifyProperty;
import org.openlumify.core.model.properties.types.StringSingleValueOpenLumifyProperty;

public class GraphProductOntology {
    public static final String IRI = "http://openlumify.org/workspace/product/graph";
    public static final String CONCEPT_TYPE_COMPOUND_NODE = "http://openlumify.org/workspace/product/graph#compoundNode";

    public static final GraphPositionSingleValueOpenLumifyProperty ENTITY_POSITION = new GraphPositionSingleValueOpenLumifyProperty("http://openlumify.org/workspace/product/graph#entityPosition");
    public static final StringSingleValueOpenLumifyProperty PARENT_NODE = new StringSingleValueOpenLumifyProperty("http://openlumify.org/workspace/product/graph#parentNode");
    public static final StringListSingleValueOpenLumifyProperty NODE_CHILDREN = new StringListSingleValueOpenLumifyProperty("http://openlumify.org/workspace/product/graph#nodeChildren");
    public static final StringSingleValueOpenLumifyProperty NODE_TITLE = new StringSingleValueOpenLumifyProperty("http://openlumify.org/workspace/product/graph#nodeTitle");

}
