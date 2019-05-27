package org.openlumify.web.product.map;

import org.visallo.webster.Handler;
import org.openlumify.core.model.Description;
import org.openlumify.core.model.Name;
import org.openlumify.web.OpenLumifyCsrfHandler;
import org.openlumify.web.WebApp;
import org.openlumify.web.WebAppPlugin;
import org.openlumify.web.product.map.routes.RemoveVertices;
import org.openlumify.web.product.map.routes.UpdateVertices;

import javax.servlet.ServletContext;

@Name("Product: Map")
@Description("Map visualization for entities containing geolocation data")
public class MapWebAppPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        Class<? extends Handler> authenticationHandlerClass = authenticationHandler.getClass();
        Class<? extends Handler> csrfHandlerClass = OpenLumifyCsrfHandler.class;

        app.post("/product/map/vertices/remove", authenticationHandlerClass, csrfHandlerClass, RemoveVertices.class);
        app.post("/product/map/vertices/update", authenticationHandlerClass, csrfHandlerClass, UpdateVertices.class);

        app.registerJavaScript("/org/openlumify/web/product/map/plugin.js");
        app.registerJavaScript("/org/openlumify/web/product/map/detail/pluginGeoShapeDetail.js", true);

        app.registerCompiledJavaScript("/org/openlumify/web/product/map/dist/geoShapePreview.js");
        app.registerCompiledJavaScript("/org/openlumify/web/product/map/dist/MapLayersContainer.js");
        app.registerCompiledJavaScript("/org/openlumify/web/product/map/dist/Map.js");
        app.registerCompiledJavaScript("/org/openlumify/web/product/map/dist/actions-impl.js");

        app.registerCompiledWebWorkerJavaScript("/org/openlumify/web/product/map/dist/plugin-worker.js");

        app.registerResourceBundle("/org/openlumify/web/product/map/messages.properties");

        app.registerLess("/org/openlumify/web/product/map/layers/mapLayers.less");
        app.registerLess("/org/openlumify/web/product/map/detail/geoShapeDetail.less");
    }
}
