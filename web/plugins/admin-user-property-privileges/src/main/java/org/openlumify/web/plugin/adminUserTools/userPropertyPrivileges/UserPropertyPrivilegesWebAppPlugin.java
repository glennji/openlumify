package org.openlumify.web.plugin.adminUserTools.userPropertyPrivileges;

import org.visallo.webster.Handler;
import org.openlumify.core.model.Description;
import org.openlumify.core.model.Name;
import org.openlumify.web.OpenLumifyCsrfHandler;
import org.openlumify.web.WebApp;
import org.openlumify.web.WebAppPlugin;
import org.openlumify.web.privilegeFilters.AdminPrivilegeFilter;

import javax.servlet.ServletContext;

@Name("Admin User Tools: User Property Privileges")
@Description("Admin tools to manage privileges stored in a property on the user")
public class UserPropertyPrivilegesWebAppPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        Class<? extends Handler> authenticationHandlerClass = authenticationHandler.getClass();
        Class<? extends Handler> csrfHandlerClass = OpenLumifyCsrfHandler.class;

        app.registerJavaScript("/org/openlumify/web/plugin/adminUserTools/userPropertyPrivileges/plugin.js", true);
        app.registerWebWorkerJavaScript(
                "/org/openlumify/web/plugin/adminUserTools/userPropertyPrivileges/userAdminPrivilegesService.js"
        );
        app.registerJavaScriptComponent(
                "/org/openlumify/web/plugin/adminUserTools/userPropertyPrivileges/UserAdminPrivilegesPlugin.jsx"
        );
        app.registerResourceBundle("/org/openlumify/web/plugin/adminUserTools/userPropertyPrivileges/messages.properties");

        app.post(
                "/user/privileges/update",
                authenticationHandlerClass,
                csrfHandlerClass,
                AdminPrivilegeFilter.class,
                UserUpdatePrivileges.class
        );
    }
}
