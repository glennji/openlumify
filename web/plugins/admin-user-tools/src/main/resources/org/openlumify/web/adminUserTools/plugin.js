define([
    'configuration/plugins/registry',
    'util/messages'
], function(registry, i18n) {
    'use strict';

    const adminExtensionPoint = 'org.openlumify.admin';

    /**
     * @undocumented
     */
    registry.documentExtensionPoint(
        'org.openlumify.admin.user.privileges',
        "Displays editor for user's privileges.",
        function() {
            return true;
        }
    );

    /**
     * @undocumented
     */
    registry.documentExtensionPoint(
        'org.openlumify.admin.user.authorizations',
        "Displays editor for user's authorizations.",
        function() {
            return true;
        }
    );

    registry.registerExtension(adminExtensionPoint, {
        componentPath: 'org/openlumify/web/adminUserTools/UserAdminPlugin',
        section: i18n('admin.user.section'),
        name: i18n('admin.user.editor'),
        subtitle: i18n('admin.user.editor.subtitle')
    });

    registry.registerExtension(adminExtensionPoint, {
        componentPath: 'org/openlumify/web/adminUserTools/ActiveUserList',
        section: i18n('admin.user.section'),
        name: i18n('admin.user.activeList'),
        subtitle: i18n('admin.user.activeList.subtitle')
    });
});
