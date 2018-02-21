define([
    'util/component/attacher',
    'configuration/plugins/registry',
    'components/visibility/VisibilityViewer',
    'components/visibility/VisibilityEditor'
], function(Attacher, registry, VisibilityViewer, VisibilityEditor) {
    'use strict';

    /**
     * Plugin to configure the user interface for displaying and editing visibility authorization strings.
     *
     * Accepts component paths for one or both of the visibility viewer and visibility editor components
     *
     * @param {string} editorComponentPath The path to {@link org.visallo.visibility~Editor} component
     * @param {string} viewerComponentPath The path to {@link org.visallo.visibility~Viewer} component
     */
    registry.documentExtensionPoint('org.visallo.visibility',
        'Implement custom interface for visibility display and editing',
        function(e) {
            return (_.isUndefined(e.editorComponentPath) || _.isString(e.editorComponentPath))
                && (_.isUndefined(e.viewerComponentPath) || _.isString(e.viewerComponentPath))
        },
        'http://docs.visallo.org/extension-points/front-end/visibility'
    );

    let warnMultipleExtensions = true;
    const checkForMultipleExtensions = () => {
        let visibilityExtensions = registry.extensionsForPoint('org.visallo.visibility');
        if (visibilityExtensions.length > 1) {
            console.warn('Multiple visibility extensions loaded', visibilityExtensions);
        }
        warnMultipleExtensions = false;
    };

    return {
        attachComponent: function(type, node, params) {
            if (warnMultipleExtensions) {
                checkForMultipleExtensions();
            }

            const Component = type === 'viewer' ? VisibilityViewer : VisibilityEditor;
            const attacher = Attacher().node(node).component(Component).params(params);

            attacher.teardown({ react: true, flight: true });
            attacher.attach();

            return attacher;
        }
    };
});
