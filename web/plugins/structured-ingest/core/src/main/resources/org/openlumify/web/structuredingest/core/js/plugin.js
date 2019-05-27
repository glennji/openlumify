
define([
    'configuration/plugins/registry',
    'util/withDataRequest'
], function(registry, withDataRequest) {
    'use strict';


    withDataRequest.dataRequest('org-openlumify-structuredingest', 'mimeTypes')
        .then(function(result) {
            registry.registerExtension('org.openlumify.detail.text', {
                shouldReplaceTextSectionForVertex: function(vertex) {
                    var vertexMimeType = _.findWhere(vertex.properties, { name: 'http://openlumify.org#mimeType' });
                    var vertexMimeTypeLower = vertexMimeType && vertexMimeType.value && vertexMimeType.value.toLowerCase();
                    var foundExtension = vertexMimeTypeLower && _.any(result.mimeTypes, function(mimeType) {
                         return mimeType.toLowerCase() === vertexMimeTypeLower
                    });

                    return foundExtension;
                },
                componentPath: 'org/openlumify/web/structuredingest/core/js/TextSection'
            });
        });

    registry.registerExtension('org.openlumify.activity', {
        type: 'org-openlumify-structured-ingest',
        kind: 'longRunningProcess',
        titleRenderer: function(el, process) {
            require([
                'util/withDataRequest',
                'util/vertex/formatters'
            ], function(withDataRequest, F) {
                withDataRequest.dataRequest('vertex', 'store', {
                    workspaceId: process.workspaceId,
                    vertexIds: process.vertexId
                }).done(function(vertex) {
                    if (!_.isArray(vertex)) {
                        var title = F.vertex.title(vertex);

                        el.title = title;
                        el.textContent = i18n('activity.tasks.type.org-openlumify-structured-ingest.import', F.string.truncate(title, 12));
                    }
                });
            });
        },
        finishedComponentPath: 'org/openlumify/web/structuredingest/core/js/structuredFileImportAcivityFinished'
    })

    require.config({
        paths: {
            velocity: 'org/openlumify/web/structuredingest/core/libs/velocity.min',
            'velocity-ui': 'org/openlumify/web/structuredingest/core/libs/velocity.ui.min'
        },
        shims: {
            velocity: { deps: ['jquery'] },
            'velocity-ui': { deps: ['velocity'] }
        }
    });

});
