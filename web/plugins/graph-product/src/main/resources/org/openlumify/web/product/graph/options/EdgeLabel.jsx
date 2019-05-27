define([
    'create-react-class'
], function(createReactClass) {
    'use strict';

    const preferenceName = 'edgeLabels';
    const EdgeLabel = createReactClass({
        onChange(event) {
            const checked = event.target.checked;
            openlumifyData.currentUser.uiPreferences[preferenceName] = '' + checked;
            this.props.openlumifyApi.v1.dataRequest('user', 'preference', preferenceName, checked);
        },

        render() {
            const preferenceValue = openlumifyData.currentUser.uiPreferences[preferenceName];
            const showEdges = preferenceValue !== 'false';

            return (
                <label>{i18n('product.toolbar.edgeLabels.toggle')}
                    <input onChange={this.onChange} type="checkbox" defaultChecked={showEdges} />
                </label>
            )
        }
    });

    return EdgeLabel;
});
