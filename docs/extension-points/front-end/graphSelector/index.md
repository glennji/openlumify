# Graph Selection

* [Graph Selection JavaScript API `org.openlumify.graph.selection`](../../../javascript/org.openlumify.graph.selection.html)
* [Graph Selection Example Code](https://github.com/openlumify/doc-examples/tree/master/extension-graph-selection)

Add custom [Cytoscape](http://js.cytoscape.org/) selection menu items. Graph provides select all, none, and invert by default.

<div style="text-align:center">
<img src="./selection.png" width="100%" style="max-width: 400px;">
</div>

## Tutorial

This tutorial will create a selection menu item to select a random node or edge.

### Web Plugin

Register the plugin script in a web plugin.

{% github_embed "https://github.com/openlumify/doc-examples/blob/ba76f66a/extension-graph-selection/src/main/java/org/openlumify/examples/graph_selection/GraphSelectionWebAppPlugin.java#L17" %}{% endgithub_embed %}

### Register Extension

Register the selection extension to find a random element and select it.

{% github_embed "https://github.com/openlumify/doc-examples/blob/ba76f66a/extension-graph-selection/src/main/resources/org/openlumify/examples/graph_selection/plugin.js" %}{% endgithub_embed %}

The `elements` function in cytoscape will return all nodes and edges. This includes decorations on nodes, and temporary edges used for find path, etc. Filter by `.v,.e` to include only _real_ vertices and edges.

