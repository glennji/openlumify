
// Global Mocks
$ = function() {
    return { on: function() {} };
};
$.extend = _.extend;
window = this;
document = {};
navigator = { userAgent: ''};
openlumifyData = publicData = { currentWorkspaceId: 'WORKSPACE_ID' };
console = {
    log: print,
    info: print,
    debug: print,
    warn: consoleWarn,
    error: consoleError
};
window.addEventListener = function() { };
window.openlumifyEnvironment = { dev: false, prod: true };
require.config({
    baseUrl: '',
    paths: {
        // LIBS
        'chrono': '../libs/chrono.min',
        'sf': '../libs/sf',
        'timezone-js': '../libs/date',
        'underscore': '../libs/underscore',
        'bluebird': '../libs/promise-6.0.0',
        'duration-js': '../libs/duration',
        'moment': '../libs/moment-with-locales',
        'moment-timezone': '../libs/moment-timezone-with-data',
        'weak-map': '../libs/weakmap',

        // MOCKS
        'jquery': 'mocks/jquery',
        'jstz': 'mocks/jstz',
        'util/withDataRequest': 'mocks/withDataRequest',
        'store': 'mocks/store',
        'reselect': 'mocks/reselect',

        // SRC
        'util/formatters': 'util_formatters',
        'util/promise': 'util_promise',
        'util/messages': 'util_messages',
        'util/parsers': 'util_parsers',
        'util/requirejs/promise': 'util_requirejs_promise',
        'util/service/messagesPromise': 'util_service_messagesPromise',
        'util/service/propertiesPromise': 'util_service_propertiesPromise',
        'util/vertex/formatters': 'util_vertex_formatters',
        'util/vertex/formula': 'util_vertex_formula',
        'util/vertex/urlFormatters': 'util_vertex_urlFormatters',
        'service/config': 'service/config',
        'data/web-worker/store/ontology/selectors': 'store_ontology_selectors'
    },
    shims: {
        'bluebird': { exports: 'Promise' },
        'util/vertex/formatters': { deps: ['util/promise'] }
    }
});

var timerLoop = makeWindowTimer(this, function () { });

require(['util/promise', 'weak-map'], function(Promise) {
    openlumifyData.storePromise = new Promise(function(r) {
        require(['store'], function(_store) {
            r(_store.getStore())
        })
    })
    define('util/visibility/util', [], {});

    require(['util/vertex/formatters'], function(F) {
        var createFunction = function(name) {
                return function(json) {
                     return F.vertex[name](JSON.parse(json));
                }
            };

        window.evaluateTitleFormulaJson = createFunction('title');
        window.evaluateTimeFormulaJson = createFunction('time');
        window.evaluateSubtitleFormulaJson = createFunction('subtitle');
        window.evaluatePropertyFormulaJson = function(json, propertyKey, propertyName) {
            return F.vertex['prop'](JSON.parse(json), propertyName, propertyKey);
        }
    });
});

timerLoop();
