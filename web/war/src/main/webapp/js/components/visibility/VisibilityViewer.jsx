define([
    'create-react-class',
    'prop-types',
    '../Attacher',
    '../RegistryInjectorHOC'
], function(
    createReactClass,
    PropTypes,
    Attacher,
    RegistryInjectorHOC) {
    'use strict';

    const DEFAULT_VIEWER = 'components/visibility/default/VisibilityViewer';

    /**
     * @typedef org.visallo.visibility~Viewer
     * @property {string} [value] The visibility source to view
     * @property {string} [property] The property that this visibility is
     * attached. Could be undefined
     * @property {string} [element] The element that the visibility is a part
     * of. Could be undefined
     */
    const VisibilityViewer = createReactClass({
        propTypes: {
            value: PropTypes.string,
            property: PropTypes.object,
            element: PropTypes.object
        },

        render() {
            const { registry, ...props } = this.props;
            const extensions = _.values(registry['org.visallo.visibility']).map(e => e.viewerComponentPath);
            const componentPath = extensions[0] || DEFAULT_VIEWER;

            return <Attacher componentPath={componentPath} {...props} />
        }
    });

    return RegistryInjectorHOC(VisibilityViewer, [
        'org.visallo.visibility'
    ]);
});
