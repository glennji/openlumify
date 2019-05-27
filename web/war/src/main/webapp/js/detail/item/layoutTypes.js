define([
    'configuration/plugins/registry'
], function(registry) {
    'use strict';

    var register = _.partial(registry.registerExtension, 'org.openlumify.layout.type');

    [
        {
            type: 'flex',
            componentPath: 'detail/item/types/flex'
        }
    ].forEach(register)
});
