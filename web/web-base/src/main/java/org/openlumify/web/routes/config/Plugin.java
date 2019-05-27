package org.openlumify.web.routes.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.webster.ParameterizedHandler;
import org.openlumify.webster.annotations.Handle;
import org.openlumify.webster.annotations.Required;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.web.OpenLumifyResponse;

import javax.servlet.http.HttpServletRequest;
import java.io.File;

@Singleton
public class Plugin implements ParameterizedHandler {
    private static final String WEB_PLUGINS_PREFIX = "web.plugins.";
    private static final String DEFAULT_PLUGINS_DIR = "/jsc/configuration/plugins";
    private final org.openlumify.core.config.Configuration configuration;

    @Inject
    public Plugin(org.openlumify.core.config.Configuration configuration) {
        this.configuration = configuration;
    }

    @Handle
    public void handle(
            HttpServletRequest request,
            @Required(name = "pluginName") String pluginName,
            OpenLumifyResponse response
    ) throws Exception {
        final String configurationKey = WEB_PLUGINS_PREFIX + pluginName;
        String pluginPath = configuration.get(configurationKey, null);

        // Default behavior if not customized
        if (pluginPath == null) {
            pluginPath = request.getServletContext().getResource(DEFAULT_PLUGINS_DIR + "/" + pluginName).getPath();
        }

        String uri = request.getRequestURI();
        String searchString = "/" + pluginName + "/";
        String pluginResourcePath = uri.substring(uri.indexOf(searchString) + searchString.length());

        if (pluginResourcePath.endsWith(".js")) {
            response.setContentType("application/x-javascript");
        } else if (pluginResourcePath.endsWith(".ejs")) {
            response.setContentType("text/plain");
        } else if (pluginResourcePath.endsWith(".css")) {
            response.setContentType("text/css");
        } else if (pluginResourcePath.endsWith(".html")) {
            response.setContentType("text/html");
        } else {
            throw new OpenLumifyResourceNotFoundException("Only js,ejs,css,html files served from plugin");
        }

        String filePath = FilenameUtils.concat(pluginPath, pluginResourcePath);
        File file = new File(filePath);

        if (!file.exists()) {
            throw new OpenLumifyResourceNotFoundException("Could not find file: " + filePath);
        }

        response.setCharacterEncoding("UTF-8");
        FileUtils.copyFile(file, response.getOutputStream());
    }
}
