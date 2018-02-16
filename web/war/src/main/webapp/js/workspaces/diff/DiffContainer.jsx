define([
    'react-redux',
    'data/web-worker/store/selection/actions',
    'data/web-worker/store/selection/selectors',
    'data/web-worker/store/user/selectors',
    'data/web-worker/store/ontology/selectors',
    './DiffPanel'
], function(
    redux,
    selectionActions,
    selectionSelectors,
    userSelectors,
    ontologySelectors,
    DiffPanel
) {

    const DiffContainer = redux.connect(

        (state, props) => {
            var concepts = ontologySelectors.getConcepts(state),
                properties = ontologySelectors.getProperties(state),
                relationships = ontologySelectors.getRelationships(state),
                selection = selectionSelectors.getSelectionMap(state),
                privileges = userSelectors.getPrivileges(state),
                workspace = state.workspace.byId[state.workspace.currentId];

            return {
                ...props,
                concepts,
                properties,
                relationships,
                selection,
                privileges,
                workspace
            }
        },

        function(dispatch, props) {
            return {
                onSetSelection: (selection) => dispatch(selectionActions.set(selection)),
                onClearSelection: () => dispatch(selectionActions.clear()),

                onVertexMenu: (element, vertexId, position) => {
                    $(element).trigger('showVertexContextMenu', { vertexId, position });
                },
                onEdgeMenu: (element, edgeIds, position) => {
                    $(element).trigger('showEdgeContextMenu', { edgeIds, position });
                }
            }
        }
    )(DiffPanel);

    return DiffContainer;
});
