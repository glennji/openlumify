define(['classnames'], function(classNames) {

    const DiffActions = function(props) {
        const {
            publish = false,
            undo = false,
            requiresOntologyPublish = false,
            diff,
            privileges,
            ontology,
            onPublishClick,
            onUndoClick
        } = props;

        if (!privileges['EDIT']) {
            return null;
        }

        const disabledBecauseOntologyChange = requiresOntologyPublish && !privileges['ONTOLOGY_PUBLISH'];
        const canPublish = privileges['PUBLISH'] && !disabledBecauseOntologyChange;
        const publishClass = classNames('btn', 'btn-mini', 'publish', { 'btn-success': publish });
        const undoClass = classNames('btn', 'btn-mini', 'undo', { 'btn-danger': undo });

        return (
            <div className="actions">
                <div className="btn-group">
                    { canPublish ? (
                        <button className={publishClass} onClick={onPublishClick}>
                            {i18n('workspaces.diff.button.publish')}
                        </button>
                    ) : null}
                    <button className={undoClass} onClick={onUndoClick}>
                        {i18n('workspaces.diff.button.undo')}
                    </button>
                </div>
            </div>
        )
    };

    return DiffActions;
});
