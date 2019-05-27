package org.openlumify.web.plugin.adminUserTools.userPropertyAuth;

import org.visallo.webster.Handler;
import org.openlumify.core.model.Description;
import org.openlumify.core.model.Name;
import org.openlumify.web.OpenLumifyCsrfHandler;
import org.openlumify.web.WebApp;
import org.openlumify.web.WebAppPlugin;
import org.openlumify.web.privilegeFilters.AdminPrivilegeFilter;

import javax.servlet.ServletContext;

@Name("Admin User Tools: User Property Authorization")
@Description("Admin tools to manage authorizations stored in a property on the user")
public class UserPropertyAuthorizationWebAppPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        Class<? extends Handler> authenticationHandlerClass = authenticationHandler.getClass();
        Class<? extends Handler> csrfHandlerClass = OpenLumifyCsrfHandler.class;

        app.registerJavaScript("/org/openlumify/web/plugin/adminUserTools/userPropertyAuth/plugin.js", true);
        app.registerJavaScriptComponent(
                "/org/openlumify/web/plugin/adminUserTools/userPropertyAuth/UserAdminAuthorizationPlugin.jsx"
        );
        app.registerWebWorkerJavaScript("/org/openlumify/web/plugin/adminUserTools/userPropertyAuth/userAdminAuthorizationService.js");
        app.registerResourceBundle("/org/openlumify/web/plugin/adminUserTools/userPropertyAuth/messages.properties");

        app.post(
                "/user/auth/add",
                authenticationHandlerClass,
                csrfHandlerClass,
                AdminPrivilegeFilter.class,
                UserAddAuthorization.class
        );
        app.post(
                "/user/auth/remove",
                authenticationHandlerClass,
                csrfHandlerClass,
                AdminPrivilegeFilter.class,
                UserRemoveAuthorization.class
        );
    }
}
