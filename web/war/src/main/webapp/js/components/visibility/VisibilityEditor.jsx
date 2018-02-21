define([
    'create-react-class',
    'prop-types',
    'react-redux',
    'data/web-worker/store/user/selectors',
    '../Attacher',
    '../RegistryInjectorHOC'
], function(
    createReactClass,
    PropTypes,
    redux,
    userSelectors,
    Attacher,
    RegistryInjectorHOC) {
    'use strict';

    const DEFAULT_EDITOR = 'components/visibility/default/VisibilitySelector';

    /**
     * @typedef org.visallo.visibility~Editor
     * @property {string} [value] The visibility source to prepopulate the editor
     * @property {string} [placeholder] The placeholder text to display when no value
     * @property {string} [readonly] Show the form in read-only mode
     * @property {object} [authorizations] The available visibilities, defaults to the current user's visibility authorizations
     * @property {function} visibilitychange The callback to invoke when the visibility is changed
     */
    const VisibilityEditor = createReactClass({
        propTypes: {
            value: PropTypes.string,
            placeholder: PropTypes.string,
            readonly: PropTypes.bool,
            authorizations: PropTypes.object,
            onVisibilityChange: PropTypes.func,
            legacyOnVisibilityChange: PropTypes.func
        },

        getInitialState() {
            return {
                componentPath: this.getComponentPath(this.props.registry)
            }
        },

        componentWillReceiveProps(nextProps) {
            const componentPath = this.getComponentPath(nextProps.registry);
            if (componentPath !== this.state.componentPath) {
                this.setState({ componentPath });
            }
        },

        render() {
            const { registry, onVisibilityChange, legacyOnVisibilityChange, ...passthru } = this.props;
            const { componentPath } = this.state;

            return (
                <Attacher
                    ref={r => { this._ref = r }}
                    componentPath={componentPath}
                    behavior={{
                        visibilitychange: this.onVisibilityChange
                    }}
                    {...passthru}
                />
            );
        },

        onVisibilityChange(inst, data) {
            const { onVisibilityChange, legacyOnVisibilityChange } = this.props;

            if (onVisibilityChange) {
                onVisibilityChange(data)
            } else if (legacyOnVisibilityChange) {
                legacyOnVisibilityChange(data);
            }
        },

        getComponentPath(registry) {
            const extensions = _.values(registry['org.visallo.visibility']).map(e => e.editorComponentPath);
            const componentPath = extensions[0] || DEFAULT_EDITOR;

            return componentPath;
        }
    });

    return redux.connect(
        (state, props) => {
            const authorizations = props.authorizations || userSelectors.getAuthorizations(state);

            return {
                authorizations
            };
        }
    )(RegistryInjectorHOC(VisibilityEditor, [
        'org.visallo.visibility'
    ]));
});
