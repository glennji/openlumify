package org.openlumify.web.table;

import org.visallo.webster.Handler;
import org.openlumify.core.model.Description;
import org.openlumify.core.model.Name;
import org.openlumify.web.WebApp;
import org.openlumify.web.WebAppPlugin;

import javax.servlet.ServletContext;

@Name("Table")
@Description("Provides a dashboard card for tabular saved search results")
public class TableWebAppPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {

        app.registerJavaScript("/org/openlumify/web/table/js/plugin.js", true);
        app.registerCompiledJavaScript("/org/openlumify/web/table/dist/card.js");
        app.registerJavaScriptComponent("/org/openlumify/web/table/js/card/Config.jsx");
        app.registerJavaScriptTemplate("/org/openlumify/web/table/hbs/columnConfigPopover.hbs");

        app.registerCss("/org/openlumify/web/table/node_modules/react-resizable/css/styles.css");
        app.registerLess("/org/openlumify/web/table/less/table.less");

        app.registerResourceBundle("/org/openlumify/web/table/messages.properties");

        app.registerFile("/org/openlumify/web/table/img/empty-table.png", "image/png");
    }
}
