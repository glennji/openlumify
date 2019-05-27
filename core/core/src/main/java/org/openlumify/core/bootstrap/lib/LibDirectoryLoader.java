package org.openlumify.core.bootstrap.lib;

import org.openlumify.core.config.Configuration;
import org.openlumify.core.config.FileConfigurationLoader;
import org.openlumify.core.model.Description;
import org.openlumify.core.model.Name;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;

import java.io.File;
import java.util.List;

@Name("Lib Directory")
@Description("Loads .jar files from a directory on the local file system")
public class LibDirectoryLoader extends LibLoader {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(LibDirectoryLoader.class);

    @Override
    public void loadLibs(Configuration configuration) {
        LOGGER.info("Loading libs using %s", LibDirectoryLoader.class.getName());
        List<File> libDirectories = FileConfigurationLoader.getOpenLumifyDirectoriesFromMostPriority("lib");
        for (File libDirectory : libDirectories) {
            addLibDirectory(libDirectory);
        }
    }
}
