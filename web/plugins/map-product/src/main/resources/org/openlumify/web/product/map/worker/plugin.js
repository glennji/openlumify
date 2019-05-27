define([
    'configuration/plugins/registry',
    'updeep',
    'org/openlumify/web/product/map/dist/actions-impl'
], function(registry, u, actions) {

    registry.registerExtension('org.openlumify.store', {
        key: 'product',
        reducer: function(state, { type, payload }) {
            switch (type) {
                case 'PRODUCT_MAP_ADD_ELEMENTS': return addElements(state, payload);
                case 'PRODUCT_MAP_REMOVE_ELEMENTS': return removeElements(state, payload);
                case 'PRODUCT_MAP_SET_LAYER_ORDER': return setLayerOrder(state, payload);
            }

            return state;
        },
        undoActions: {
            PRODUCT_MAP_ADD_ELEMENTS: {
                undo: (undo) => actions.removeElements(undo),
                redo: (redo) => actions.redoDropElements(redo)
            },
            PRODUCT_MAP_REMOVE_ELEMENTS: {
                undo: (undo) => actions.redoDropElements(undo),
                redo: (redo) => actions.removeElements(redo)
            }
        }
    })

    function addElements(state, { workspaceId, productId, vertexIds, elements }) {
        const product = state.workspaces[workspaceId].products[productId];
        const vertices = product && product.extendedData && product.extendedData.vertices;
        const newVertices = {};
        if (elements) {
            Object.keys(elements).forEach(key => {
                newVertices[key] = u.constant(elements[key])
            })
        }
        if (vertexIds) {
            vertexIds.forEach(id => {
                newVertices[id] = { id }
            });
        }
        if (vertices) {
            return u({
                workspaces: {
                    [workspaceId]: {
                        products: {
                            [productId]: {
                                extendedData: {
                                    vertices: newVertices
                                }
                            }
                        }
                    }
                }
            }, state);
        }

        return state;
    }

    function removeElements(state, { workspaceId, productId, elements }) {
        return u({
            workspaces: {
                [workspaceId]: {
                    products: {
                        [productId]: {
                            extendedData: {
                                vertices: u.omitBy(v => elements.vertexIds.includes(v.id))
                            }
                        }
                    }
                }
            }
        }, state);
    }

    function setLayerOrder(state, { workspaceId, productId, layerOrder }) {
        return u({
            workspaces: {
                [workspaceId]: {
                    products: {
                        [productId]: {
                            extendedData: {
                                'org-openlumify-map-layers': {
                                    layerOrder: u.constant(layerOrder)
                                }
                            }
                        }
                    }
                }
            }
        }, state);
    }
});
