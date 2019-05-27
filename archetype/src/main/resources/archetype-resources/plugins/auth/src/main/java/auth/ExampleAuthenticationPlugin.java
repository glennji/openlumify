#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.auth;

import com.google.inject.Singleton;
import com.google.inject.Inject;
import org.visallo.webster.Handler;
import org.openlumify.core.model.Description;
import org.openlumify.core.model.Name;
import org.openlumify.web.AuthenticationHandler;
import org.openlumify.web.WebApp;
import org.openlumify.web.WebAppPlugin;

import javax.servlet.ServletContext;

@Name("Example OpenLumify Authentication Plugin")
@Description("Registers an authentication plugin which demonstrates user/password login.")
@Singleton
public class ExampleAuthenticationPlugin implements WebAppPlugin {
    private final Login login;

    @Inject
    public ExampleAuthenticationPlugin(Login login) {
        this.login = login;
    }

    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        app.registerBeforeAuthenticationJavaScript("/${packageInPathFormat}/auth/plugin.js");
        app.registerJavaScript("/${packageInPathFormat}/auth/authentication.js", false);
        app.registerJavaScriptTemplate("/${packageInPathFormat}/auth/login.hbs");
        app.registerCss("/${packageInPathFormat}/auth/login.css");
        app.registerResourceBundle("/${packageInPathFormat}/auth/messages.properties");

        app.post(AuthenticationHandler.LOGIN_PATH, login);
    }
}
