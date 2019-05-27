Element Menu Plugin
===================
* [Vertex Menu Javascript API `org.openlumify.vertex.menu`](../../../javascript/org.openlumify.vertex.menu.html)
* [Edge Menu Javascript API `org.openlumify.edge.menu`](../../../javascript/org.openlumify.edge.menu.html)

Plugin to add new items to vertex or edge context menu. Providing a `shouldDisable` handler will still show the
item in the context menu, provide a `canHandle` function if you want to remove items completely based
on the current selection and target element.

To add a divider:

```js
registry.registerExtension('org.openlumify.vertex.menu', 'DIVIDER');  // vertex menu
registry.registerExtension('org.openlumify.edge.menu', 'DIVIDER');  //edge menu
```

## Example

To register an item:

{% github_embed "https://github.com/openlumify/doc-examples/blob/bab1856a/extension-element-context-menu/src/main/resources/org/openlumify/examples/context_menu/plugin.js#L1-L48", hideLines=['23-47'] %}{% endgithub_embed %}

Then add an event listener to handle when your menu item is clicked:

{% github_embed "https://github.com/openlumify/doc-examples/blob/bab1856a/extension-element-context-menu/src/main/resources/org/openlumify/examples/context_menu/plugin.js#L3-L47", hideLines=['4-35', '43-46'] %}{% endgithub_embed %}
