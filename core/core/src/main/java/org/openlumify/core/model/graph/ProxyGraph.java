package org.openlumify.core.model.graph;

import org.vertexium.Graph;

public interface ProxyGraph extends Graph {
    Graph getProxiedGraph();
}
