define([
    'util/vertex/formatters',
    'classnames',
    'components/visibility/VisibilityViewer'
], function(
    F,
    classNames,
    VisibilityViewer) {

    const DiffProperty = function(props) {
        const { diff, ontologyProperty, height, publish = false, undo = false } = props;
        const { deleted, elementId, property, previousProperty } = diff;
        const { name, value, visibilitySource } = property;
        const propertyNameDisplay = ontologyProperty && ontologyProperty.displayName || null;

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
          </div>
        );
    };

    return DiffProperty;
});

/*
renderPropertyDiff: function(key, style, property) {
    const { className, deleted, id, name, new: nextProp, old: previousProp, publish, undo } = property;
    const { formatLabel } = this.props;
    const nextVisibility = nextProp ? formatVisibility(nextProp) : null;
    const visibility = previousProp ? formatVisibility(previousProp) : null;
    const nextValue = nextProp ? formatValue(name, nextProp, property) : null;
    const value = previousProp ? formatValue(name, previousProp, property) : null;
    const valueStyle = value !== nextValue ? { textDecoration: 'line-through'} : {};
    const visibilityStyle = visibility !== nextVisibility ? { textDecoration: 'line-through'} : {};
    const propertyNameDisplay = formatLabel(name);

    return (
        <div key={key} style={style}
            className={classNames('d-row', className, {
                'mark-publish': publish,
                'mark-undo': undo
            })}
            data-diff-id={id}>
        <div title={propertyNameDisplay} className="property-label">{ propertyNameDisplay }</div>
        <div title={nextValue} className={classNames('property-value', { deleted: deleted })}>
            {previousProp && nextProp ? (
                [
                    nextValue,
                    <VisibilityViewer key={key + 'p-vis'} value={nextVisibility && nextVisibility.source} />,
                    <div title={value} key={key + 'pval'} style={valueStyle}>{value}</div>,
                    <VisibilityViewer key={key + 'p-val-vis'} style={visibilityStyle} value={visibility && visibility.source} />
                ]
            ) : null}
            {!previousProp && nextProp ? (
                [
                    nextValue,
                    <VisibilityViewer key={key + 'v'} value={nextVisibility && nextVisibility.source} />
                ]
            ) : null}
        </div>
            {this.renderRequiresOntologyPublish(property)}
            {this.renderDiffActions(id, property)}
      </div>
    );
},

*/
