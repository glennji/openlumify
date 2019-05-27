define([
    'create-react-class',
    'prop-types',
    'public/v1/api',
    'components/Alert'
], function(
    createReactClass,
    PropTypes,
    openlumify,
    Alert) {

    const UserAdminAuthorizationPlugin = createReactClass({
        propTypes: {
            // The user for which the authorizations will be edited
            user: PropTypes.shape({
                userName: PropTypes.string.isRequired,
                authorizations: PropTypes.array.isRequired
            })
        },

        dataRequest: null,

        getInitialState() {
            return {
                error: null,
                authorizations: this.props.user.authorizations,
                saveInProgress: false,
                addAuthorizationValue: ''
            };
        },

        componentWillMount() {
            openlumify.connect()
                .then(({dataRequest})=> {
                    this.dataRequest = dataRequest;
                })
        },

        componentWillReceiveProps(nextProps) {
            this.setState({
                error: null,
                authorizations: nextProps.user.authorizations,
                saveInProgress: false,
                addAuthorizationValue: ''
            });
        },

        handleAddAuthorizationSubmit(e) {
            e.preventDefault();
            this.addAuthorization(this.state.addAuthorizationValue);
        },

        addAuthorization(authorization){
            this.setState({
                saveInProgress: true
            });

            this.dataRequest('com-openlumify-userAdminAuthorization', 'userAuthAdd', this.props.user.userName, authorization)
                .then((user) => {
                    this.setState({
                        addAuthorizationValue: '',
                        authorizations: user.authorizations,
                        saveInProgress: false,
                        error: null
                    });
                })
                .catch((e) => {
                    this.setState({error: e, saveInProgress: false});
                });
        },

        handleAuthorizationDeleteClick(authorization) {
            this.setState({
                saveInProgress: true
            });

            this.dataRequest('com-openlumify-userAdminAuthorization', 'userAuthRemove', this.props.user.userName, authorization)
                .then((user) => {
                    this.setState({
                        authorizations: user.authorizations,
                        saveInProgress: false,
                        error: null
                    });
                })
                .catch((e) => {
                    this.setState({error: e, saveInProgress: false });
                });
        },

        handleAlertDismiss() {
            this.setState({
                error: null
            });
        },

        handleAddAuthorizationInputChange(e) {
            this.setState({
                addAuthorizationValue: e.target.value
            });
        },

        render() {
            return (
                <div>
                    <div className="nav-header">{i18n('admin.user.editor.userAdminAuthorization.authorizations')}</div>
                    <Alert error={this.state.error} onDismiss={this.handleAlertDismiss}/>
                    <ul>
                        { this.state.authorizations.map((auth) => (
                            <li key={auth} className="auth-item highlight-on-hover">
                                <button className="btn btn-mini btn-danger show-on-hover"
                                        onClick={()=>this.handleAuthorizationDeleteClick(auth)}
                                        disabled={this.state.saveInProgress}>
                                    {i18n('admin.user.editor.userAdminAuthorization.deleteAuthorization')}
                                </button>
                                <span style={{lineHeight: '1.2em'}}>{auth}</span>
                            </li>
                        )) }
                    </ul>

                    <form onSubmit={this.handleAddAuthorizationSubmit}>
                        <input style={{marginTop: '0.5em'}}
                               className="auth"
                               placeholder={i18n('admin.user.editor.userAdminAuthorization.addAuthorizationPlaceholder')}
                               type="text"
                               value={this.state.addAuthorizationValue}
                               onChange={this.handleAddAuthorizationInputChange}
                               disabled={this.state.saveInProgress}/>
                        <button
                            className="btn"
                            disabled={this.state.saveInProgress}>
                            {i18n('admin.user.editor.userAdminAuthorization.addAuthorization')}
                        </button>
                    </form>
                </div>
            );
        }
    });

    return UserAdminAuthorizationPlugin;
});
