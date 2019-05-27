define([
    'public/v1/api'
], function(openlumify) {
    'use strict';

    var adminUserAuthorizationsExtensionPoint = 'org.openlumify.admin.user.authorizations';

    openlumify.registry.registerExtension(adminUserAuthorizationsExtensionPoint, {
        componentPath: 'org/openlumify/web/plugin/adminUserTools/userPropertyAuth/UserAdminAuthorizationPlugin'
    });
});
