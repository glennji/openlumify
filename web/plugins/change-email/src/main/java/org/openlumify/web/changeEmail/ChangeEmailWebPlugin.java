package org.openlumify.web.changeEmail;

import org.visallo.webster.Handler;
import org.visallo.webster.handlers.StaticResourceHandler;
import org.openlumify.core.model.Description;
import org.openlumify.core.model.Name;
import org.openlumify.web.OpenLumifyCsrfHandler;
import org.openlumify.web.WebApp;
import org.openlumify.web.WebAppPlugin;
import org.openlumify.web.privilegeFilters.ReadPrivilegeFilter;

import javax.servlet.ServletContext;

@Name("Change E-Mail")
@Description("Allows a user to change their e-mail address")
public class ChangeEmailWebPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        Class<? extends Handler> authenticationHandlerClass = authenticationHandler.getClass();
        Class<? extends Handler> csrfHandlerClass = OpenLumifyCsrfHandler.class;

        app.registerJavaScript("/org/openlumify/web/changeEmail/changeEmail.js");
        app.registerResourceBundle("/org/openlumify/web/changeEmail/messages.properties");

        app.registerJavaScriptTemplate("/org/openlumify/web/changeEmail/template.hbs");

        app.post("/changeEmail", authenticationHandlerClass, csrfHandlerClass, ReadPrivilegeFilter.class, ChangeEmail.class);
    }
}
