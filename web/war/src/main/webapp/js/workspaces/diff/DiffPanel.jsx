define([
    'create-react-class',
    'prop-types',
    'classnames',
    'react-virtualized',
    'util/vertex/formatters',
    'util/privileges',
    'util/dnd',
    'components/visibility/VisibilityViewer',
    './DiffVertex',
    './DiffEdge',
    './DiffHeader'
], function(
    createReactClass,
    PropTypes,
    classNames,
    ReactVirtualized,
    F,
    Privileges,
    dnd,
    VisibilityViewer,
    DiffVertex,
    DiffEdge,
    DiffHeader
) {
    'use strict';

    const THING_IRI = 'http://www.w3.org/2002/07/owl#Thing';
    const ELEMENT_SIZE = 45;
    const PROPERTY_UPDATE_SIZE = 75;
    const PROPERTY_NEW_SIZE = 40;
    const AVERAGE_ROW_SIZE = Math.round((ELEMENT_SIZE + PROPERTY_UPDATE_SIZE + PROPERTY_NEW_SIZE) / 3);
    //const diffs = [];
    //const loadedRows = {};
    //const edgeVerticesById = {};
    //const vertexEdgesById = {};
    const ACTIONS = {
        Publish: 1,
        Undo: 2,
    }

    var loadMoreTimeout, initial = true;

    function formatVisibility(propertyOrProperties) {
        const property = Array.isArray(propertyOrProperties) ? propertyOrProperties[0] : propertyOrProperties;
        return property['http://visallo.org#visibilityJson'];
    }

    function formatValue(name, change, property) {
        return F.vertex.prop({
            id: property.id,
            properties: change ? Array.isArray(change) ? change : [change] : []
        }, name, property.key)
    }

    const DiffPanel = createReactClass({

        propTypes: {
            total: PropTypes.number.isRequired
        },

        getInitialState() {
            return {
                search: {},
                all: false,
                vertexIds: {},
                edgeIds: {}
                //publish: { all: false, vertexIds: {}, edgeIds: {} },
                //undo: { all: false, vertexIds: {}, edgeIds: {} }
            };
        },

        isRowLoaded({ index }) {
            return !!this.loadedRows[index];
        },

        loadMoreRows({ startIndex, stopIndex }) {
            const { search: { query, invalid, count } } = this.state;

            if (loadMoreTimeout) {
                clearTimeout(loadMoreTimeout)
            }

            var promiseResolver;
            const promise = new Promise(resolve => {
                promiseResolver = resolve
            });

            loadMoreTimeout = setTimeout(() => {
                let start = startIndex;
                let stop = stopIndex;
                for (let i = startIndex; i <= stopIndex; i++) {
                    if (this.loadedRows[i]) {
                        start++;
                    } else {
                        break;
                    }
                }
                for (let i = stopIndex; i >= startIndex; i--) {
                    if (this.loadedRows[i]) {
                        stop--;
                    } else {
                        break;
                    }
                }
                // Server expects an exclusive stop index
                stop++;
                if ((stop - start) > 0) {
                    Promise.require('util/withDataRequest')
                        .then(dr => dr.dataRequest('workspace', 'diff', start, stop, query))
                        .then(response => {
                            for (let i = start, j = 0; i < stop; i++, j++) {
                                this.loadedRows[i] = true
                                this.diffs[i] = response.diffs[j]
                            }
                            Object.assign(this.edgeVerticesById, response.edgeVerticesById)
                            Object.assign(this.vertexEdgesById, response.vertexEdgesById)
                            if (promiseResolver) {
                                promiseResolver();
                            }
                        })
                }
            }, initial ? 0 : 500);

            initial = false;

            return promise;
        },

        onRowClick({ vertexId, edgeId }) {
            const { selection } = this.props;
            if (vertexId in selection || edgeId in selection) {
                this.props.onClearSelection()
            } else if (vertexId) {
                this.props.onSetSelection({ vertices: [vertexId] })
            } else if (edgeId) {
                this.props.onSetSelection({ edges: [edgeId] })
            }
        },

        onDragStart(event, { vertexId, edgeId }) {
            if (vertexId || edgeId) {
                const elements = { vertexIds: vertexId ? [vertexId] : [], edgeIds: edgeId ? [edgeId] : [] };
                const dt = event.dataTransfer;
                if (dt) {
                    dnd.setDataTransferWithElements(dt, elements);
                }
            }
        },

        _onMark(defaultAction, marks, property) {
            const { all, vertexIds, edgeIds } = this.state;
            const nextState = { all, vertexIds, edgeIds };

            (_.isArray(marks) ? marks : [marks]).forEach(function handleMark({
                vertexId,
                edgeId,
                action = defaultAction,
                allowInvert = true,
                warnTooMany,
                sync = []
            }) {
                const { all, vertexIds, edgeIds } = nextState;
                const id = vertexId || edgeId;

                console.log(action, vertexId ? 'v' : 'e', id)
                //if (action === null) debugger

                if (id) {
                    const map = vertexId ? vertexIds : edgeIds;
                    const otherMap = vertexId ? edgeIds : vertexIds;
                    const mapName = vertexId ? 'vertexIds' : 'edgeIds';
                    const otherMapName = vertexId ? 'edgeIds' : 'vertexIds';
                    const elementState = map[id];
                    const invert = (elementState && elementState.action) === action;
                    const existingSync = elementState && elementState.sync || [];
                    const omitOtherActions = map => _.omit(map, ({ action: a }) => action !== a);

                    nextState[otherMapName] = omitOtherActions(otherMap);

                    if (!action || (invert && allowInvert)) {
                        const removed = { ...map };
                        delete removed[id];
                        nextState[mapName] = removed;
                    } else {
                        const newSync = [...sync, ...existingSync];
                        nextState[mapName] = {
                            ...omitOtherActions(map),
                            [id]: { action, warnTooMany, sync: _.uniq(newSync, ({ vertexId, edgeId }) => vertexId || edgeId) }
                        };
                    }
                    if (existingSync && existingSync.length) {
                        console.log('apply existing sync', id, existingSync, action)
                        const isMarked = (action) => ({ vertexId, edgeId }) => {
                            return (
                                vertexId ?
                                (nextState.vertexIds[vertexId] && nextState.vertexIds[vertexId].action) :
                                (nextState.edgeIds[edgeId] && nextState.edgeIds[edgeId].action)
                            ) === action;
                        };
                        existingSync.forEach(({fn}) => {
                            const newMark = fn(isMarked(ACTIONS.Publish), isMarked(ACTIONS.Undo))
                            if (newMark) {
                                console.log('Should do', newMark)
                                handleMark(newMark)
                            }
                        })
                    }
                }
            });

            if (!_.isEmpty(nextState)) {
                this.setState(nextState);
            }
        },

        onMarkPublish(marks) {
            this._onMark(ACTIONS.Publish, marks);
        },

        onMarkUndo(marks) {
            this._onMark(ACTIONS.Undo, marks);
        },

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

        componentWillMount() {
            this.loadedRows = {};
            this.diffs = []
            this.edgeVerticesById = {};
            this.vertexEdgesById = {};
        },

        componentDidMount() {
            this.scrollTop = 0;
        },

        componentDidUpdate(prevProps, prevState) {
            const { total } = this.props;
            const { total: previousTotal } = prevProps;
            const List = this._List;
            const InfiniteLoader = this._InfiniteLoader;

            if (List) {
                if (total !== previousTotal) {
                    //List.recomputeRowHeights();
                }
                if (this.scrollTop > 0) {
                    // HACK: Need to pass decimal to force update. Have to look at virtualized-grid
                    List.scrollToPosition(this.scrollTop + 0.1);
                }
            }
            if (InfiniteLoader) {
                const { search: { query } } = this.state;
                const { search: { query: oldQuery } } = prevState;

                if (query !== oldQuery) {
                    console.log('reseting')
                    InfiniteLoader.resetLoadMoreRowsCache(true);
                }
            }
        },

        render() {
            const { AutoSizer, InfiniteLoader, List } = ReactVirtualized;
            const { total, selection, privileges, workspace: { editable } } = this.props;
            const { search, vertexIds, edgeIds, all } = this.state;
            const { query, searching, count, invalid } = search;
            const reRenderProps = { query, selection, privileges, editable, vertexIds, edgeIds, all };
            const rowHeight = ELEMENT_SIZE;

            let remoteRowCount = total;
            if (query && count !== null) {
                remoteRowCount = count;
            }

            const counts = Object.values(vertexIds).concat(Object.values(edgeIds)).reduce((sums, elementState) => {
                const action = elementState && elementState.action;
                if (action === ACTIONS.Publish) {
                    sums.publishCount++;
                } else if (action === ACTIONS.Undo) {
                    sums.undoCount++;
                    if (elementState && elementState.warnTooMany) {
                        sums.warnTooMany += elementState.warnTooMany;
                    }
                }
                return sums;
            }, { publishCount: 0, undoCount: 0, warnTooMany: 0})

            return (
                <div className="diffs-list">
                    <DiffHeader
                        editable={editable}
                        {...counts}
                        search={search}
                        totalCount={remoteRowCount}
                        privileges={privileges}
                        onSearchChange={this.onSearchChange}
                    />
                    <div className="diff-cont">
                        <div className="diff-alerts"></div>
                        <div className="diff-content">
                            { searching ? (
                                <div style={{
                                    position: 'absolute',
                                    left: 0,
                                    right: 0,
                                    textAlign: 'center',
                                    top: '50%',
                                    transform: 'translate(0, -50%)'
                                }}>{i18n('workspaces.diff.searching')}</div>
                            ) : (
                                <InfiniteLoader
                                    ref={r => { this._InfiniteLoader = r; } }
                                    isRowLoaded={this.isRowLoaded}
                                    loadMoreRows={this.loadMoreRows}
                                    rowCount={remoteRowCount}
                                    threshold={5}
                                    minimumBatchSize={10}
                                >
                                    {({ onRowsRendered, registerChild }) => (
                                        <AutoSizer>
                                        {({ height, width }) => {
                                            return (
                                            <List
                                                ref={r => {
                                                    registerChild(r)
                                                    this._List = r;
                                                }}
                                                {...reRenderProps}
                                                width={width}
                                                height={height}
                                                overscanRowCount={5}
                                                rowCount={remoteRowCount}
                                                rowHeight={rowHeight}
                                                rowRenderer={this.rowRenderer}
                                                noRowsRenderer={this.noRowsRenderer}
                                                onRowsRendered={onRowsRendered}
                                                onScroll={this.onScroll} />
                                            )
                                        }}
                                        </AutoSizer>
                                    )}
                                </InfiniteLoader>
                            )}
                        </div>
                    </div>
                </div>
            );
        },

        onScroll({ scrollTop }) {
            this.scrollTop = scrollTop;
        },

        onSearchChange(event) {
            const query = event.target.value;
            const InfiniteLoader = this._InfiniteLoader;

            if (this.searchChangeTimeout) {
                clearTimeout(this.searchChangeTimeout);
            }
            this.currentSearch = query;
            if (query) {
                this.setState({ search: { searching: true }})
                this.searchChangeTimeout = setTimeout(() => {
                    Promise.require('util/withDataRequest')
                        .then(dr => dr.dataRequest('workspace', 'diffCount', query))
                        .then(result => {
                            if (this.currentSearch === query) {
                                initial = true;
                                this.searchReplaced = { diffs: this.diffs, loadedRows: this.loadedRows };
                                this.diffs = [];
                                this.loadedRows = {};
                                //InfiniteLoader.resetLoadMoreRowsCache(true);
                                this.setState({ search: { query, count: result.total }})
                            }
                        })
                        .catch(error => {
                            console.error(error)
                            this.setState({ search: { invalid: true }})
                        })
                }, 500);
            } else {
                this.diffs = [];
                this.loadedRows = {};
                //InfiniteLoader.resetLoadMoreRowsCache(true);
                this.setState({ search: {} })
            }
        },

        noRowsRenderer() {
            return (<div style={{
                position: 'absolute',
                left: 0,
                right: 0,
                textAlign: 'center',
                top: '50%',
                transform: 'translate(0, -50%)'
            }}>{i18n('workspaces.diff.none')}</div>);
        },

        rowRenderer({ index, isScrolling, isVisible, key, parent, style }) {
            const diff = this.diffs[index];
            if (diff && diff.type) {
                const { selection, workspace: { editable }, concepts, properties, relationships, privileges } = this.props;
                const { vertexIds, edgeIds, all } = this.state;
                const element = diff.vertex || diff.edge;
                const id = element && element.id;
                const elementState = (diff.vertex ? vertexIds : edgeIds)[id] || false;
                const elementAction = elementState && elementState.action;
                const props = {
                    diff,
                    privileges,
                    editable,
                    properties,
                    publish: elementAction === ACTIONS.Publish,
                    undo: elementAction === ACTIONS.Undo,
                    ontology: (
                        diff.vertex ? concepts[diff.vertex.conceptType || THING_IRI] :
                        diff.edge ? relationships[diff.edge.label] : null
                    ),
                    onRowClick: this.onRowClick,
                    onDragStart: this.onDragStart,
                    onPublish: this.onMarkPublish,
                    onUndo: this.onMarkUndo,
                };

                props.requiresOntologyPublish = this.checkIfRequiresOntologyPublish(diff, props.ontology)

                if (diff.type === 'VertexDiffItem') {
                    const unpublishedEdges = diff.vertex.edgeInfos && diff.vertex.edgeInfos.reduce((m, info) => {
                        const edge = this.vertexEdgesById[info.edgeId];
                        if (edge && edge.sandboxStatus === 'PRIVATE') {
                            m[m.unpublishedEdgesPrefix + info.edgeId] = true;
                        }
                        return m
                    }, { unpublishedEdgesPrefix: 'UE' })
                    return (
                        <div key={key} style={style}>
                            <DiffVertex {...props}
                                {...unpublishedEdges}
                                active={diff.vertex.id in selection} />
                        </div>
                    );
                }

                if (diff.type === 'EdgeDiffItem') {
                    return (
                        <div key={key} style={style}>
                            <DiffEdge {...props}
                                active={diff.edge.id in selection}
                                outDiff={this.edgeVerticesById[diff.edge.outVertexId]}
                                inDiff={this.edgeVerticesById[diff.edge.inVertexId]} />
                        </div>
                    );
                }
                throw new Error('Unknown type: ' + diff.type)
            }
            return (
                <div key={key} style={style} className="d-row vertex-row">
                    <div className="vertex-label">
                        <div className="img"></div>
                        <h1>{i18n('workspaces.diff.title.loading')}</h1>
                    </div>
                </div>
            );
        },

        checkIfRequiresOntologyPublish(diff, ontology) {
            const { concepts, properties } = this.props;
            return (
                isSandboxed(ontology) ||
                (diff.vertex && anyPropertySandboxed(diff.vertex, properties)) ||
                (diff.edge && (
                    isSandboxed(concepts[this.edgeVerticesById[diff.edge.outVertexId].vertex.conceptType || THING_IRI]) ||
                    isSandboxed(concepts[this.edgeVerticesById[diff.edge.inVertexId].vertex.conceptType || THING_IRI]) ||
                    anyPropertySandboxed(diff.edge, properties) ||
                    anyPropertySandboxed(this.edgeVerticesById[diff.edge.outVertexId].vertex, properties) ||
                    anyPropertySandboxed(this.edgeVerticesById[diff.edge.inVertexId].vertex, properties)
                ))
            );
        }
    });

    return DiffPanel;

    function isSandboxed(o) {
        return o.sandboxStatus !== 'PUBLIC';
    }

    function anyPropertySandboxed(el, properties) {
        return el.properties.some(p => isSandboxed(properties[p.name]));
    }
});
