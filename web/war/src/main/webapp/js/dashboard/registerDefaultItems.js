define(['configuration/plugins/registry'], function(registry) {
    'use strict';

    registry.registerExtension('org.openlumify.dashboard.item', {
        title: i18n('dashboard.savedsearches.title'),
        description: i18n('dashboard.savedsearches.description'),
        identifier: 'org-openlumify-web-saved-search',
        componentPath: 'search/dashboard/savedSearch',
        configurationPath: 'search/dashboard/configure',
        grid: {
            width: 4,
            height: 4
        }
    });

    registry.registerExtension('org.openlumify.dashboard.item', {
        title: i18n('dashboard.notifications.title'),
        description: i18n('dashboard.notifications.description'),
        identifier: 'org-openlumify-web-notifications',
        componentPath: 'notifications/dashboardItem',
        grid: {
            width: 3,
            height: 3
        }
    });

    registry.registerExtension('org.openlumify.dashboard.toolbar.item', {
        identifier: 'org-openlumify-notification-clear-all',
        canHandle: function(options) {
            return options.extension.identifier === 'org-openlumify-web-notifications'
        },
        tooltip: i18n('dashboard.notifications.clearall.hover'),
        icon: 'img/trash.png',
        action: {
            type: 'event',
            name: 'notificationClearAll'
        }
    });

    registry.registerExtension('org.openlumify.dashboard.item', {
        title: i18n('dashboard.pie.entity.title'),
        description: i18n('dashboard.pie.entity.description'),
        identifier: 'org-openlumify-web-dashboard-concept-counts',
        report: {
            defaultRenderer: 'org-openlumify-pie',
            endpoint: '/vertex/search',
            endpointParameters: {
                q: '*',
                size: 0,
                filter: '[]',
                aggregations: [
                    {
                        type: 'term',
                        name: 'field',
                        field: 'http://openlumify.org#conceptType'
                    }
                ].map(JSON.stringify)
            }
        },
        grid: {
            width: 4,
            height: 2
        }
    });

    registry.registerExtension('org.openlumify.dashboard.item', {
        title: i18n('dashboard.pie.edge.title'),
        description: i18n('dashboard.pie.edge.description'),
        identifier: 'org-openlumify-web-dashboard-edge-counts',
        report: {
            defaultRenderer: 'org-openlumify-pie',
            endpoint: '/edge/search',
            endpointParameters: {
                q: '*',
                size: 0,
                filter: '[]',
                aggregations: [
                    {
                        type: 'term',
                        name: 'field',
                        field: '__edgeLabel'
                    }
                ].map(JSON.stringify)
            }
        },
        grid: {
            width: 4,
            height: 2
        }
    });

    registry.registerExtension('org.openlumify.dashboard.item', {
        title: i18n('dashboard.welcome.title'),
        description: i18n('dashboard.welcome.description'),
        identifier: 'org-openlumify-web-dashboard-welcome',
        componentPath: 'dashboard/items/welcome/welcome',
        options: {
            preventDefaultConfig: true
        },
        grid: {
            width: 5,
            height: 4
        }
    });
})
