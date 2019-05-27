/*eslint no-labels:0*/
define([
    'flight/lib/component',
    'util/withDataRequest'
], function(defineComponent, withDataRequest) {
    'use strict';

    return defineComponent(StructuredFileImportAcivityFinished, withDataRequest);

    function StructuredFileImportAcivityFinished() {

        this.after('teardown', function() {
            this.$node.empty();
        });

        this.defaultAttrs({
            searchRelatedSelector: '.search-related',
            selectVertexSelector: '.select-vertex'
        });

        this.after('initialize', function() {
            this.on('click', {
                searchRelatedSelector: this.onSearchRelatedClick,
                selectVertexSelector: this.onSelectVertex
            });

            this.loadDefaultContent();


            this.on(document, 'workspaceLoaded', this.onWorkspaceLoaded);
            this.trigger(document, 'updateDiff');

            this.req = this.dataRequest('longRunningProcess', 'get', this.attr.process.id)
            this.req.then(results => {
                require(['data/web-worker/store/element/actions'], actions => {
                    openlumifyData.storePromise.then(store => store.dispatch(actions.refreshElement({
                        workspaceId: this.attr.process.workspaceId,
                        vertexId: results.vertexId
                    })));
                });
            })
        });

        this.loadDefaultContent = function() {
            var $searchRelatedButton = $('<button>').addClass('search-related btn btn-mini')
                    .text(i18n('activity.tasks.type.com-openlumify-structuredFile.searchRelated')),
                $selectVertex = $('<button>').addClass('select-vertex btn btn-mini')
                    .text(i18n('activity.tasks.type.com-openlumify-structuredFile.selectVertex'));

            this.$node.empty().append($searchRelatedButton).append($selectVertex);

            this.updateButtons(openlumifyData.currentWorkspaceId);
        }

        this.updateButtons = function(workspaceId) {
            var self = this,
                onDifferentWorkspace = workspaceId !== self.attr.process.workspaceId,
                $searchRelatedButton = self.select('searchRelatedSelector'),
                $selectVertexButton = self.select('selectVertexSelector');

            $selectVertexButton.prop('disabled', false);
            $selectVertexButton.attr('title', i18n('activity.tasks.type.com-openlumify-structuredFile.selectVertex'));

            if (onDifferentWorkspace) {
                $searchRelatedButton.prop('disabled', true);
                $searchRelatedButton.attr('title', i18n('activity.tasks.type.com-openlumify-structuredFile.searchRelatedDifferentWorkspace'));
            } else {
                $searchRelatedButton.prop('disabled', false);
                $searchRelatedButton.attr('title', i18n('activity.tasks.type.com-openlumify-structuredFile.searchRelated'));
            }
        };

        this.onWorkspaceLoaded = function(event, data) {
            this.updateButtons(data.workspaceId);
        };

        this.onSelectVertex = function(event) {
            var $target = $(event.target).addClass('loading').prop('disabled', true);

            this.req.then(results => {
                $target.removeClass('loading').prop('disabled', false);
                this.trigger('selectObjects', { vertexIds: [results.vertexId] });
            })
        };

        this.onSearchRelatedClick = function(event) {
            var $target = $(event.target).addClass('loading').prop('disabled', true);

            this.req.then(results => {
                $target.removeClass('loading').prop('disabled', false);
                this.trigger('searchRelated', { vertexId: results.vertexId });
            });
        };
    }
});
