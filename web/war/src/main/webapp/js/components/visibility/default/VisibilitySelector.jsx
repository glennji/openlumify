define([
    'create-react-class',
    'prop-types',
    'react-virtualized-select'
], function(
    createReactClass,
    PropTypes,
    { default: VirtualizedSelect }) {


    const defaultVisibilityOption = { label: i18n('visibility.blank'), value: '' };

    const VisibilitySelector = createReactClass({
        propTypes: {
            value: PropTypes.string,
            placeholder: PropTypes.string,
            readonly: PropTypes.bool,
            creatable: PropTypes.bool,
            disabled: PropTypes.bool,
            clearable: PropTypes.bool,
            authorizations: PropTypes.object,
            visibilitychange: PropTypes.func
        },

        getDefaultProps() {
            return {
                placeholder: i18n('visibility.label'),
                creatable: false
            }
        },

        getInitialState() {
            const { value, authorizations } = this.props;
            const options = this.mapAuthorizationsToOptions(authorizations);

            return {
                value,
                options,
                valid: this.checkValid(value)
            }
        },

        componentWillReceiveProps(nextProps) {
            if (nextProps.value !== this.state.value) {
                this.setState({ value: nextProps.value, valid: this.checkValid(nextProps.value) })
            }

            if (nextProps.authorizations !== this.props.authorizations) {
                this.setState({ options: this.mapAuthorizationsToOptions(nextProps.authorizations)})
            }
        },

        render() {
            const { placeholder, disabled, clearable, visibilitychange, authorizations, value: initialValue, ...passthru } = this.props;
            const { options, value, valid } = this.state;

            return (
                <VirtualizedSelect
                    ref={r => { this._Virtualized = r }}
                    className={'visibility-selector'}
                    simpleValue
                    matchProp={'label'}
                    clearable={clearable === undefined ? Boolean(value) : clearable}
                    searchable
                    disabled={disabled}
                    placeholder={placeholder}
                    promptTextCreator={label => i18n('visibility.selector.prompt', label) }
                    value={value}
                    resetValue={''}
                    options={options}
                    optionHeight={28}
                    autoBlur={true}
                    onChange={this.onChange}
                    onInputKeyDown={this.onInputKeyDown}
                    {...passthru}
                />

            );
        },

        onChange(value) {
            const valid = this.checkValid(value);

            this.setState({ value, valid })
            this.props.visibilitychange({ value, valid })
        },

        checkValid(value) {
            const authorizations = this.props.authorizations;
            return Boolean(value === '' || value === undefined || value in authorizations);
        },

        mapAuthorizationsToOptions(authorizations) {
            return _.chain(authorizations)
                .pick(value => value === true)
                .keys()
                .map(authorization => ({ label: authorization, value: authorization }))
                .sortBy(option => option.label.toLowerCase())
                .tap(auths => { auths.push(defaultVisibilityOption) })
                .value();
        }
    });

    return VisibilitySelector;
});
