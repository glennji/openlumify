
# Websocket Handlers

Extension to register new listeners for websocket messages. Must be registered in JavaScript file registered with `app.registerWebWorkerJavaScript` in web app plugin.

```js
registry.registerExtension('org.openlumify.websocket.message', {
    name: name,
    handler: function(data) {
    }
});
```

