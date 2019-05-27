package org.openlumify.web.auth.usernameonly;

import org.visallo.webster.Handler;
import org.visallo.webster.handlers.StaticResourceHandler;
import org.openlumify.core.bootstrap.InjectHelper;
import org.openlumify.core.model.Description;
import org.openlumify.core.model.Name;
import org.openlumify.web.AuthenticationHandler;
import org.openlumify.web.WebApp;
import org.openlumify.web.WebAppPlugin;
import org.openlumify.web.auth.usernameonly.routes.Login;

import javax.servlet.ServletContext;

@Name("Username Only Authentication")
@Description("Allows authenticating using just a username")
public class UsernameOnlyWebAppPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {

        app.registerBeforeAuthenticationJavaScript("/org/openlumify/web/auth/usernameonly/plugin.js");
        app.registerJavaScriptTemplate("/org/openlumify/web/auth/usernameonly/templates/login.hbs");
        app.registerJavaScript("/org/openlumify/web/auth/usernameonly/authentication.js", false);

        app.registerLess("/org/openlumify/web/auth/usernameonly/less/login.less");

        app.post(AuthenticationHandler.LOGIN_PATH, InjectHelper.getInstance(Login.class));
    }
}
