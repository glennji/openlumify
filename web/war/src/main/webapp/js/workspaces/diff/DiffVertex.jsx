define([
    'create-react-class',
    'classnames',
    'prop-types',
    'util/vertex/formatters',
    'react-pure-render-mixin',
    './DiffActions',
    './DiffSubtitle',
    './DiffToggle'
], function(
    createReactClass,
    classNames,
    PropTypes,
    F,
    ReactPureRenderMixin,
    DiffActions,
    DiffSubtitle,
    DiffToggle
) {
    const DiffVertex = createReactClass({
        mixins: [ReactPureRenderMixin],

        propTypes: {
            diff: PropTypes.shape({
                vertex: PropTypes.object.isRequired,
                sandboxStatus: PropTypes.string.isRequired,
                deleted: PropTypes.bool.isRequired,
            }).isRequired,
            onDragStart: PropTypes.func.isRequired,
            onPublish: PropTypes.func.isRequired,
            onUndo: PropTypes.func.isRequired
        },

        componentDidUpdate(prevProps, prevState) {
            const diff = (type, prev, next) => {
                if (type === 'state' && !this.state) return [];
                return Object.keys(next).reduce((result, key) => {
                    const prevValue = prev[key]
                    const value = next[key];
                    if (prevValue !== value) {
                        result.push({ type, key, value, prevValue })
                    }
                    return result
                }, [])
            }
            const diffs = diff('props', prevProps, this.props).concat(diff('state', prevState, this.state))
            console.groupCollapsed('was updated, ' + diffs.length + ', changes');
            diffs.forEach(({type, key, value, prevValue }) => {
                console.log(type + key + '=', value, 'prev?', prevValue);
            })
            console.groupEnd();
        },

        render() {
            const {
                diff,
                privileges,
                active = false,
                publish = false,
                undo = false,
                height,
                className,
                action,
                opened,
                loading,
                requiresOntologyPublish,
                ...rest } = this.props;
            const { deleted, vertex, sandboxStatus } = diff;
            const { id } = vertex;
            const title = F.vertex.title(vertex);
            const conceptImageStyle = {
                backgroundImage: `url(${F.vertex.image(vertex, null, 80)})`
            };
            const selectedConceptImageStyle = {
                backgroundImage: `url(${F.vertex.selectedImage(vertex, null, 80)})`
            };

            return (
                <div style={{height}} className={classNames('d-row', 'vertex-row', className, {
                        'mark-publish': publish,
                        'mark-undo': undo,
                        active, deleted, opened
                    })}
                    onClick={this.onRowClick}
                    onDragStart={this.onDragStart}
                    draggable={true}
                >
                    <DiffToggle height={height} onToggle={this.onToggle} opening={loading} opened={opened} />
                    <div className="vertex-label">
                        <div className="img" style={conceptImageStyle}></div>
                        <div className="selected-img" style={selectedConceptImageStyle}></div>
                        <h1 title={title}>{title}</h1>
                        <DiffSubtitle
                            privileges={privileges}
                            requiresOntologyPublish={requiresOntologyPublish}
                            sandboxStatus={sandboxStatus} />
                    </div>
                    <DiffActions
                        {...rest}
                        requiresOntologyPublish={requiresOntologyPublish}
                        diff={diff}
                        privileges={privileges}
                        publish={publish}
                        undo={undo}
                        onPublishClick={this.onPublishClick}
                        onUndoClick={this.onUndoClick}
                    />
                </div>
            );
        },

        onToggle(event) {
            event.stopPropagation();
            this.props.onToggle({ vertexId: this.props.diff.vertex.id })
        },

        onPublishClick(event) {
            event.stopPropagation();
            this.props.onPublish({ vertexId: this.props.diff.vertex.id })
        },

        onUndoClick(event) {
            event.stopPropagation();
            const { diff, unpublishedEdgesPrefix: pf, undo } = this.props;
            const { vertex } = diff;

            const marks = [{ vertexId: vertex.id }]
            if (diff.edgeCount > 10) {
                marks[0].warnTooMany = diff.edgeCount;
            }
            const allowInvert = Boolean(undo);
            if (vertex.edgeInfos) {
                vertex.edgeInfos.forEach(({edgeId}) => {
                    const key = pf + edgeId;
                    if (key in this.props) {
                        marks.push({ edgeId, action: undo ? null : 2, allowInvert })
                    }
                })
            }
            this.props.onUndo(marks)
        },

        onRowClick(event) {
            console.log('row click', event.target)
            const { onRowClick, diff: { vertex } } = this.props;
            onRowClick({ vertexId: vertex.id });
        },

        onDragStart(event) {
            const { onDragStart, diff: { vertex } } = this.props;
            onDragStart(event, { vertexId: vertex.id });
        }
    });

    return DiffVertex;
})
