require(['public/v1/api'], function(openlumify) {
    'use strict';

    openlumify.registry.registerExtension('org.openlumify.dashboard.item', {
        title: i18n('com.openlumify.table.dashboard.title'),
        description: i18n('com.openlumify.table.dashboard.description'),
        identifier: 'org-openlumify-saved-search-table',
        componentPath: 'org/openlumify/web/table/dist/card',
        configurationPath: 'org/openlumify/web/table/js/card/Config',
        grid: {
            width: 8,
            height: 4
        }
    });

    openlumify.registry.registerExtension('com.openlumify.export.transformer', {
            className: 'com.openlumify.export.transformers.SavedSearchTransform',
            canHandle: function(json) {
                if (json.extension) {
                    if (json.extension.identifier === 'org-openlumify-saved-search-table') {
                        return Boolean(json.item &&
                            json.item.configuration &&
                            json.item.configuration.searchId &&
                            json.item.configuration.searchParameters &&
                            !json.item.configuration.aggregations);
                    }
                }
                return false;
            },
            exporterConfiguration: function(json) {
                var exporterConfig = { orientation: 'landscape' };
                var configuration = json.item.configuration;
                var searchId = json.item.configuration.searchId;
                var tableSettings = searchId && configuration.tableSettings[searchId];

                if (tableSettings) {
                    var selectedTabIri = _.findKey(tableSettings, function(tab) { return tab.active || false});
                    var sheetsWithColumns = _.mapObject(tableSettings, function(tabSettings) {
                        return _.chain(tabSettings.columns)
                            .map(function(column) {return column.visible ? column.title : null})
                            .compact()
                            .value();
                    });
                    var columns = sheetsWithColumns[selectedTabIri];

                    if (columns) exporterConfig.columns = columns;

                    if (selectedTabIri && !exporterConfig.columns) {
                        exporterConfig.columnsForConceptIri = selectedTabIri;
                    }

                    exporterConfig.selectedTabIri = selectedTabIri;
                    exporterConfig.sheetsWithColumns = sheetsWithColumns;
                }

                if (configuration) {
                    var title = configuration.title || configuration.initialTitle || (
                        json.extension && json.extension.title);
                    if (title) {
                        exporterConfig.title = title;
                    }
                }

                return exporterConfig;
            },
            // Optional, transforms json into what className expects
            prepareForTransform: function(json) {
                var config = json.item.configuration,
                    model = _.pick(config, 'searchId', 'searchParameters'),
                    tableSettings = config.tableSettings,
                    selectedTabIri = tableSettings && _.findKey(tableSettings, function(tab) { return tab.active || false;});

                if (selectedTabIri) {
                    model.searchParameters.conceptType = selectedTabIri;
                }

                return model;
            }
        });

});
