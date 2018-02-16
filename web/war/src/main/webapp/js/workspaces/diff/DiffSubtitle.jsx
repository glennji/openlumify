define([
    'prop-types',
], function(PropTypes) {

    const DiffSubtitle = function(props) {
        const {
            privileges,
            requiresOntologyPublish,
            sandboxStatus
        } = props;
        return (
            <div className="diff-action">
                {(privileges['PUBLISH'] && !privileges['ONTOLOGY_PUBLISH'] && requiresOntologyPublish) ? (
                    <div title={i18n('workspaces.diff.requires.ontology.publish')} className="action-subtype">
                    { i18n('workspaces.diff.requires.ontology.publish') }
                    </div>
                ) : (
                    <span className="label action-type">{i18n(`workspaces.diff.subtitle.status.${sandboxStatus}`)}</span>
                )}
            </div>
        )
    };

    return DiffSubtitle;
});
