define([
    'public/v1/api'
], function(openlumify) {
    'use strict';

    var adminUserPrivilegesExtensionPoint = 'org.openlumify.admin.user.privileges';

    openlumify.registry.registerExtension(adminUserPrivilegesExtensionPoint, {
        componentPath: 'org/openlumify/web/plugin/adminUserTools/userPropertyPrivileges/UserAdminPrivilegesPlugin'
    });
});
