package org.openlumify.web.changePassword;

import org.visallo.webster.Handler;
import org.visallo.webster.handlers.StaticResourceHandler;
import org.openlumify.core.model.Description;
import org.openlumify.core.model.Name;
import org.openlumify.web.OpenLumifyCsrfHandler;
import org.openlumify.web.WebApp;
import org.openlumify.web.WebAppPlugin;
import org.openlumify.web.privilegeFilters.ReadPrivilegeFilter;

import javax.servlet.ServletContext;

@Name("Change Password")
@Description("Allows a user to change their password")
public class ChangePasswordWebPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        Class<? extends Handler> authenticationHandlerClass = authenticationHandler.getClass();
        Class<? extends Handler> csrfHandlerClass = OpenLumifyCsrfHandler.class;

        app.registerJavaScript("/org/openlumify/web/changePassword/changePassword.js");
        app.registerCss("/org/openlumify/web/changePassword/changePassword.css");
        app.registerResourceBundle("/org/openlumify/web/changePassword/messages.properties");

        app.registerJavaScriptTemplate("/org/openlumify/web/changePassword/template.hbs");

        app.post("/changePassword", authenticationHandlerClass, csrfHandlerClass, ReadPrivilegeFilter.class, ChangePassword.class);
    }
}
