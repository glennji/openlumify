define(['prop-types'], function(PropTypes) {
    'use strict';

    /**
     * @typedef org.visallo.visibility~Viewer
     * @property {string} [value] The visibility source to view
     * @property {string} [property] The property that this visibility is
     * attached. Could be undefined
     * @property {string} [element] The element that the visibility is a part
     * of. Could be undefined
     */
    const VisibilityViewer = ({ value, property, element, style }) => (
        <div className="visibility" style={style}>
            {_.isUndefined(value) || value === '' ?
                (<i>{i18n('visibility.blank')}</i>) :
                value
            }
        </div>
    );

    VisibilityViewer.propTypes = {
        value: PropTypes.string,
        style: PropTypes.object
    };

    return VisibilityViewer;
});
