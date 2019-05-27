package org.openlumify.web.auth.usernamepassword;

import com.google.inject.Inject;
import org.openlumify.core.config.Configuration;
import org.openlumify.core.model.Description;
import org.openlumify.core.model.Name;
import org.visallo.webster.Handler;
import org.visallo.webster.handlers.StaticResourceHandler;
import org.openlumify.core.bootstrap.InjectHelper;
import org.openlumify.web.WebApp;
import org.openlumify.web.WebAppPlugin;
import org.openlumify.web.WebConfiguration;
import org.openlumify.web.auth.usernamepassword.routes.Login;
import org.openlumify.web.auth.usernamepassword.routes.ChangePassword;
import org.openlumify.web.auth.usernamepassword.routes.LookupToken;
import org.openlumify.web.auth.usernamepassword.routes.RequestToken;
import org.openlumify.web.AuthenticationHandler;

import javax.servlet.ServletContext;

@Name("Username/Password Authentication")
@Description("Allows authenticating using a username and password")
public class UsernamePasswordWebAppPlugin implements WebAppPlugin {
    public static final String LOOKUP_TOKEN_ROUTE = "/forgotPassword";
    public static final String CHANGE_PASSWORD_ROUTE = "/forgotPassword/changePassword";
    private Configuration configuration;

    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        app.registerBeforeAuthenticationJavaScript("/org/openlumify/web/auth/usernamepassword/plugin.js");
        app.registerJavaScriptTemplate("/org/openlumify/web/auth/usernamepassword/templates/login.hbs");
        app.registerJavaScript("/org/openlumify/web/auth/usernamepassword/authentication.js", false);

        app.registerLess("/org/openlumify/web/auth/usernamepassword/less/login.less");

        app.post(AuthenticationHandler.LOGIN_PATH, InjectHelper.getInstance(Login.class));

        ForgotPasswordConfiguration forgotPasswordConfiguration = new ForgotPasswordConfiguration();
        configuration.setConfigurables(forgotPasswordConfiguration, ForgotPasswordConfiguration.CONFIGURATION_PREFIX);
        configuration.set(WebConfiguration.PREFIX + ForgotPasswordConfiguration.CONFIGURATION_PREFIX + ".enabled", forgotPasswordConfiguration.isEnabled());
        if (forgotPasswordConfiguration.isEnabled()) {
            app.post("/forgotPassword/requestToken", RequestToken.class);
            app.get(LOOKUP_TOKEN_ROUTE, LookupToken.class);
            app.post(CHANGE_PASSWORD_ROUTE, ChangePassword.class);
        }
    }

    @Inject
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
}
