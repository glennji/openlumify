define([
    'create-react-class',
    'classnames',
    'prop-types',
    'util/vertex/formatters',
    'react-pure-render-mixin',
    './DiffActions',
    './DiffToggle'
], function(
    createReactClass,
    classNames,
    PropTypes,
    F,
    ReactPureRenderMixin,
    DiffActions,
    DiffToggle
) {
    const DiffEdge = createReactClass({
        mixins: [ReactPureRenderMixin],

        propTypes: {
            diff: PropTypes.shape({
                edge: PropTypes.object.isRequired,
                sandboxStatus: PropTypes.string.isRequired,
                deleted: PropTypes.bool.isRequired,
            }).isRequired,
            ontology: PropTypes.object,
            outDiff: PropTypes.object,
            inDiff: PropTypes.object,
            onDragStart: PropTypes.func.isRequired
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
                active = false,
                publish = false,
                undo = false,
                privileges,
                diff,
                inDiff,
                outDiff,
                ontology,
                requiresOntologyPublish,
                height,
                className,
                action,
                opened,
                loading,
                ...rest
            } = this.props;
            const { deleted, edge, sandboxStatus } = diff;
            const { id, label } = edge;
            const sourceTitle = F.vertex.title(outDiff && outDiff.vertex);
            const targetTitle = F.vertex.title(inDiff && inDiff.vertex);
            const edgeLabel = ontology && ontology.displayName || i18n('ontology.unknown');
            const title = `"${sourceTitle}" \n${edgeLabel} \n"${targetTitle}"`;

            // TODO: is deleted?

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
                        <h1 title={title} className="edge-cont">
                            <span className="edge-v">{sourceTitle}</span>
                            <span className="edge-label">{edgeLabel + ' '}</span>
                            <span className="edge-v">{targetTitle}</span>
                        </h1>
                        <div className="diff-action">
                            {(<span className="label action-type">{ sandboxStatus }</span>)}
                            { privileges['PUBLISH'] && !privileges['ONTOLOGY_PUBLISH'] && requiresOntologyPublish ? (
                                <div className="action-subtype">{ i18n('workspaces.diff.requires.ontology.publish') }</div>
                            ) : null}
                        </div>
                    </div>
                    <DiffActions
                        {...rest}
                        requiresOntologyPublish={requiresOntologyPublish}
                        diff={diff}
                        ontology={ontology}
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
            this.props.onToggle({ edgeId: this.props.diff.edge.id })
        },

        onPublishClick(event) {
            event.stopPropagation();
            const { publish, outDiff, inDiff } = this.props;
            const edgeId = this.props.diff.edge.id;
            const allowInvert = Boolean(publish)
            const sync = !publish ? [{ edgeId, fn: (isMarkedPublish, isMarkedUndo) => {
                if (isMarkedPublish({ edgeId }) &&
                    (outDiff.sandboxStatus === 'PRIVATE' &&
                    !isMarkedPublish({ vertexId: outDiff.vertex.id})) ||
                    (inDiff.sandboxStatus === 'PRIVATE' &&
                    !isMarkedPublish({ vertexId: inDiff.vertex.id}))) {
                    return { edgeId, action: null };
                }
            }}] : undefined;
            const marks = [{ edgeId }]
            if (outDiff.sandboxStatus === 'PRIVATE') {
                marks.push({ vertexId: outDiff.vertex.id, allowInvert, sync })
            }
            if (inDiff.sandboxStatus === 'PRIVATE') {
                marks.push({ vertexId: inDiff.vertex.id, allowInvert, sync });
            }
            this.props.onPublish(marks);
        },

        onUndoClick(event) {
            event.stopPropagation();
            this.props.onUndo({ edgeId: this.props.diff.edge.id })
        },

        onRowClick() {
            const { onRowClick, diff: { edge } } = this.props;
            onRowClick({ edgeId: edge.id });
        },

        onDragStart(event) {
            const { onDragStart, diff: { edge } } = this.props;
            onDragStart(event, { edgeId: edge.id });
        }
    });

    return DiffEdge;
})
