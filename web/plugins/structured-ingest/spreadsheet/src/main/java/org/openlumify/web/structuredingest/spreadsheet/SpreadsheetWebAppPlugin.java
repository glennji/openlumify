package org.openlumify.web.structuredingest.spreadsheet;

import org.visallo.webster.Handler;
import org.openlumify.core.model.Description;
import org.openlumify.core.model.Name;
import org.openlumify.web.WebApp;
import org.openlumify.web.WebAppPlugin;

import javax.servlet.ServletContext;

@Name("Structured File CSV and Excel support")
@Description("Adds support for importing structured data from CSV and Excel files")
public class SpreadsheetWebAppPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {

        app.registerJavaScript("/org/openlumify/web/structuredingest/spreadsheet/plugin.js");

    }
}
