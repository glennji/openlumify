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

    const VisibilityViewer = createReactClass({
        propTypes: {
            //
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
