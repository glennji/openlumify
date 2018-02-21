define([
    'create-react-class',
    'util/vertex/formatters',
    'classnames',
    'components/visibility/VisibilityViewer',
    './DiffActions'
], function(
    createReactClass,
    F,
    classNames,
    VisibilityViewer,
    DiffActions
) {

    const DiffProperty = createReactClass({
        render() {
            const {
                diff,
                height,
                ontology,
                publish = false, undo = false,
                ...rest
            } = this.props;
            const { deleted, elementId, property, previousProperty } = diff;
            const { name, value, visibilitySource } = property;
            const propertyNameDisplay = ontology && ontology.displayName || null;

            const formattedValue = F.vertex.propDisplay(name, value);
            const previousFormattedValue = previousProperty ?
                F.vertex.propDisplay(previousProperty.name, previousProperty.value) : null
            const valueStyle = {};//value !== nextValue ? { textDecoration: 'line-through'} : {};
            const visibilityStyle = {};// visibility !== nextVisibility ? { textDecoration: 'line-through'} : {};
            const cls = classNames('d-row', { 'mark-publish': publish, 'mark-undo': undo });

            return (
                <div style={{height: `${height}px`}} className={cls}>
                    <div title={propertyNameDisplay} className="property-label">{ propertyNameDisplay }</div>
                    <div title={formattedValue} className={classNames('property-value', { deleted })}>
                        {previousProperty ? (
                            <React.Fragment>
                                {formattedValue}
                                <VisibilityViewer value={visibilitySource} />
                                <div title={previousFormattedValue} style={valueStyle}>{previousFormattedValue}</div>
                                <VisibilityViewer style={visibilityStyle} value={previousProperty.visibilitySource} />
                            </React.Fragment>
                        ) : (
                            <React.Fragment>
                                {formattedValue}
                                <VisibilityViewer value={visibilitySource} />
                            </React.Fragment>
                        )}
                    </div>
                    <DiffActions
                        {...rest}
                        diff={diff}
                        ontology={ontology}
                        publish={publish}
                        undo={undo}
                        onPublishClick={this.onPublishClick}
                        onUndoClick={this.onUndoClick}
                    />
              </div>
            );
        },

        onPublishClick() {
        },

        onUndoClick() {
        }
    });

    return DiffProperty;
});
