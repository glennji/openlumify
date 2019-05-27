package org.openlumify.web.structuredingest.parquet;

import org.visallo.webster.Handler;
import org.openlumify.core.model.Description;
import org.openlumify.core.model.Name;
import org.openlumify.web.WebApp;
import org.openlumify.web.WebAppPlugin;

import javax.servlet.ServletContext;

@Name("Structured File Parquet support")
@Description("Adds support for importing structured data from Parquet files")
public class ParquetWebAppPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {

        app.registerJavaScript("/org/openlumify/web/structuredingest/parquet/js/plugin.js");
        app.registerJavaScript("/org/openlumify/web/structuredingest/parquet/js/textSection.js", false);
    }
}
