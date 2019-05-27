require(['configuration/plugins/registry', 'data/web-worker/store/actions'], function(registry, actions) {
    registry.registerExtension('org.openlumify.workproduct', {
        identifier: 'org.openlumify.web.product.map.MapWorkProduct',
        componentPath: 'org/openlumify/web/product/map/dist/Map',
        storeActions: actions.createActions({
            workerImpl: 'org/openlumify/web/product/map/dist/actions-impl',
            actions: {
                removeElements: function(productId, elements, undoable) { return { productId, elements, undoable }},
                dropElements: function(productId, elements, undoable) { return { productId, elements }}
            }
        })
    })

    $(document).on('applicationReady currentUserOpenLumifyDataUpdated', function() {
        $(document).trigger('registerKeyboardShortcuts', {
            scope: ['map.help.scope'].map(i18n),
            shortcuts: {
                'meta-a': { fire: 'selectAll', desc: i18n('openlumify.help.select_all') },
                'delete': { fire: 'deleteSelected', desc: i18n('openlumify.help.delete') },
                'alt-t': { fire: 'searchTitle', desc: i18n('openlumify.help.search_title') },
                'alt-s': { fire: 'searchRelated', desc: i18n('openlumify.help.search_related') },
                'undo': { fire: 'undo', desc: i18n('openlumify.help.undo') },
                'redo': { fire: 'redo', desc: i18n('openlumify.help.redo') }
            }
        });
    });
});
