#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.web;

import org.visallo.webster.Handler;
import org.openlumify.core.model.Description;
import org.openlumify.core.model.Name;
import org.openlumify.web.WebApp;
import org.openlumify.web.WebAppPlugin;

import javax.servlet.ServletContext;

@Name("Example OpenLumify Web App Plugin")
@Description("Registers a detail toolbar plugin that launches a Google search for the displayed person name.")
public class ExampleWebAppPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        app.registerJavaScript("/${packageInPathFormat}/web/plugin.js", true);
        app.registerResourceBundle("/${packageInPathFormat}/web/messages.properties");
    }
}
