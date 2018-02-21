define(['classnames'], function(classNames) {

    const DiffToggle = function({ height, onToggle, opening, opened }) {
        return (
            <button
                style={{height}}
                onClick={onToggle}
                title={i18n(`workspaces.diff.button.toggle.${!opened}`)}
                className={classNames('toggle', {
                    opening,
                    opened
                })}>{opened ? '▼' : '▶'}</button>
        )
    };

    return DiffToggle;
});
