define(['configuration/plugins/registry'], function(registry) {
    'use strict';

    registry.registerExtension('org.openlumify.authentication', {
        componentPath: 'org/openlumify/web/auth/usernamepassword/authentication'
    })
});
