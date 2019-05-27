package org.openlumify.web.ingest.cloud.s3;

import org.visallo.webster.Handler;
import org.openlumify.core.model.Description;
import org.openlumify.core.model.Name;
import org.openlumify.web.OpenLumifyCsrfHandler;
import org.openlumify.web.WebApp;
import org.openlumify.web.WebAppPlugin;
import org.openlumify.web.ingest.cloud.s3.routes.S3DirectoryListing;
import org.openlumify.web.privilegeFilters.ReadPrivilegeFilter;

import javax.servlet.ServletContext;

@Name("Amazon S3 Cloud Ingest")
@Description("Adds support for importing structured data from Amazon S3 buckets")
public class AmazonS3WebAppPlugin implements WebAppPlugin {

    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        Class<? extends Handler> authenticator = authenticationHandler.getClass();
        Class<? extends Handler> csrfProtector = OpenLumifyCsrfHandler.class;

        app.registerJavaScript("/org/openlumify/web/ingest/cloud/s3/js/plugin.js", true);
        app.registerCompiledJavaScript("/org/openlumify/web/ingest/cloud/s3/dist/Config.js");
        app.registerCompiledJavaScript("/org/openlumify/web/ingest/cloud/s3/dist/BasicAuth.js");
        app.registerCompiledJavaScript("/org/openlumify/web/ingest/cloud/s3/dist/SessionAuth.js");
        app.registerCompiledJavaScript("/org/openlumify/web/ingest/cloud/s3/dist/actions-impl.js");

        app.registerCompiledWebWorkerJavaScript("/org/openlumify/web/ingest/cloud/s3/dist/plugin-worker.js");

        app.registerLess("/org/openlumify/web/ingest/cloud/s3/style.less");
        app.registerResourceBundle("/org/openlumify/web/ingest/cloud/s3/messages.properties");

        app.post("/org/openlumify/web/ingest/cloud/s3", authenticator, csrfProtector, ReadPrivilegeFilter.class, S3DirectoryListing.class);
    }
}
