define(['reselect'], function(reselect) {
    const { createSelector } = reselect;

    const root = state => state.selection.idsByType;

    const getVertexIds = state => root(state).vertices;

    const getEdgeIds = state => root(state).edges;

    const getSelectionVertexMap = createSelector([getVertexIds], vertices => {
        const sel = {};
        vertices.forEach(v => { sel[v] = true; })
        return sel;
    });

    const getSelectionEdgeMap = createSelector([getEdgeIds], edges => {
        const sel = {};
        edges.forEach(e => { sel[e] = true; })
        return sel;
    });

    const getSelectionMap = createSelector([getSelectionVertexMap, getSelectionEdgeMap], (vertices, edges) => {
        return { ...vertices, ...edges }
    });

    return {
        getVertexIds,
        getEdgeIds,
        getSelectionMap,
        getSelectionVertexMap,
        getSelectionEdgeMap
    }
});
