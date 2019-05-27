define(['data/web-worker/store/actions'], function(actions) {
    actions.protectFromWorker();

    return actions.createActions({
        workerImpl: 'org/openlumify/web/product/map/dist/actions-impl',
        actions: {
            dropElements: (productId, elements, { undoable }) => ({ productId, elements, undoable }),
            removeElements: (productId, elements, { undoable }) => ({ productId, elements, undoable }),

            setLayerOrder: (productId, layerOrder) => ({ productId, layerOrder })
        }
    })
})
