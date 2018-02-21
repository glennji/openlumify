define(['prop-types'], function(PropTypes) {
    'use strict';

    const VisibilityViewer = ({ value, style }) => (
        <div className="visibility" style={style}>
            {_.isUndefined(value) || value === '' ?
                (<i>{i18n('visibility.blank')}</i>) :
                value
            }
        </div>
    );

    VisibilityViewer.propTypes = {
        value: PropTypes.string,
        style: PropTypes.object,
    };

    return VisibilityViewer;
});
