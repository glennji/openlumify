define([], function() {
    'use strict';

    return withPublicApi;

    function withPublicApi() {

        this.before('initialize', function() {
            this.openlumifyData = {};
            window.openlumifyData = this.openlumifyData;
        });

        this.after('initialize', function() {
            this.on(document, 'setPublicApi', this.onSetPublicApi);
        });

        this.onSetPublicApi = function(event, data) {
            this.setPublicApi(data.key, data.obj);
        };

        this.setPublicApi = function(key, obj, options) {
            options = _.extend({
                    onlyIfNull: false
                }, options || {});

            if (options.onlyIfNull && (key in this.openlumifyData)) {
                return;
            }

            if (typeof obj === 'undefined') {
                delete this.openlumifyData[key];
            } else {
                this.openlumifyData[key] = obj;
                this.trigger(key + 'OpenLumifyDataUpdated', {
                    key: key,
                    object: obj
                });
            }

            this.worker.postMessage({
                type: 'publicApi',
                key: key,
                obj: obj
            });
        }
    }
});
