package org.openlumify.web.product.graph.model;

import org.openlumify.core.model.workspace.product.WorkProductExtendedData;

import java.util.Map;

public class GraphWorkProductExtendedData extends WorkProductExtendedData {
    private Map<String, GraphWorkProductVertex> compoundNodes;

    public void setCompoundNodes(Map<String, GraphWorkProductVertex> compoundNodes) {
        this.compoundNodes = compoundNodes;
    }

    public Map<String, GraphWorkProductVertex> getCompoundNodes() {
        return compoundNodes;
    }
}
