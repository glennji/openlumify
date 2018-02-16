define(['classnames', 'util/formatters'], function(classNames, F) {

    const DiffHeader = function(props) {
        const {
            publishCount = 0,
            undoCount = 0,
            warnTooMany,
            totalCount,
            ontologyRequiredCount = 0,
            publishing,
            undoing,
            privileges,
            search: { query, searchInvalid },
            onSearchChange,
            onApplyPublishClick,
            onApplyUndoClick
        } = props;
        const totalPublishCount = !privileges['ONTOLOGY_PUBLISH'] ? (totalCount - ontologyRequiredCount) : totalCount;
        const publishingAll = publishCount > 0 && publishCount === totalPublishCount;
        const undoingAll = undoCount > 0 && undoCount === totalCount;

        return (
          <div className="diff-header">
              <h1 className="do-actions">
                {publishing || publishCount > 0 ? (
                    <button className={
                        classNames('btn btn-small publish-all btn-success', {
                            loading: publishing
                        })}
                        onClick={onApplyPublishClick}
                        disabled={publishing || undoing}
                        data-count={F.number.pretty(publishCount)}>
                        { i18n('workspaces.diff.button.publish') }
                    </button>
                ) : null}
                {undoing || undoCount > 0 ? (
                    <button className={
                        classNames('btn btn-small undo-all btn-danger', {
                            loading: undoing
                        })}
                        onClick={onApplyUndoClick}
                        disabled={publishing || undoing}
                        title={warnTooMany ?
                            i18n('workspaces.diff.button.undo.warnTooMany',
                                F.number.pretty(undoCount),
                                F.number.pretty(warnTooMany)
                            ) : null
                        }
                        data-count={F.number.pretty(undoCount + warnTooMany)}>
                        { i18n('workspaces.diff.button.undo') }
                    </button>
                ) : null}
              </h1>
              <input
                  type="search"
                  className={classNames('search-query', { invalid: Boolean(searchInvalid) })}
                  placeholder={i18n('workspaces.diff.search.placeholder')}
                  onChange={onSearchChange}
                  defaultValue={query || ''}
              />
              {renderHeaderActions(props, publishingAll, undoingAll, publishCount + undoCount)}
          </div>
        );
    };

    function renderHeaderActions(props, publishingAll, undoingAll, showingCommitButtons) {
        const {
            editable,
            publishing,
            undoing,
            privileges,
            onSelectAllPublishClick,
            onSelectAllUndoClick,
            onDeselectAllClick } = props;
        const applying = publishing || undoing;

        if (!editable || !privileges['EDIT']) return null;

        return (
          <div className="select-actions">
              { showingCommitButtons ? null : (
                  <span>{ i18n('workspaces.diff.button.select_all') }</span>
              )}
              <div className="btn-group actions">
                {privileges['PUBLISH'] ? (
                    <button className={
                        classNames('btn btn-mini select-all-publish', {
                            'btn-success': publishingAll
                        })}
                        onClick={publishingAll ? onDeselectAllClick : onSelectAllPublishClick}
                        disabled={applying}
                        data-action="publish">
                        {i18n('workspaces.diff.button.publish')}
                    </button>
                ) : null}
                {privileges['EDIT'] ? (
                    <button className={
                        classNames('btn btn-mini select-all-undo', {
                            'btn-danger': undoingAll
                        })}
                        onClick={undoingAll ? onDeselectAllClick : onSelectAllUndoClick}
                        disabled={applying}
                        data-action="undo">
                        {i18n('workspaces.diff.button.undo')}
                    </button>
                ) : null}
              </div>
          </div>
        );
    }

    return DiffHeader;
});
