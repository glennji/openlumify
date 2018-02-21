# Visibility Plugin

Plugin to configure the user interface for displaying and editing visibility authorization strings.

Accepts component paths for one or both of the visibility viewer and visibility editor components:

```js
// if an editor or viewer component path is not specified a default component will be used
registry.registerExtension('org.visallo.visibility', {
    editorComponentPath: 'myEditor',
    viewerComponentPath: 'myViewer'
});
```

## Tutorial

### Visibility Editor Component

Describes the form for editing visibility values.

#### Properties

Accessible in plugin as `this.attr`

* `value`: Previous value to populate.
// placeholder, readonly

### Visibility Display Component

Describes the display of visibility values.

#### Attributes

* `value`: Current visibility value.
* `property`: Current property.
// missing element
