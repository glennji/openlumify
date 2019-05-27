package org.openlumify.core.config;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.io.FilenameUtils;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.util.ProcessUtil;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * By default searches for openlumify configuration directories in this order:
 * - Location specified by system property VISALLO_DIR
 * - Location specified by environment variable VISALLO_DIR
 * - ${user.home}/.openlumify
 * - ${appdata}/OpenLumify
 * - /opt/openlumify/ or c:/opt/openlumify/
 * <p/>
 * You can override the default search order using a system property or environment property VISALLO_CONFIGURATION_LOADER_SEARCH_LOCATIONS.
 * The default is: systemProperty,env,userHome,appdata,defaultDir
 */
public class FileConfigurationLoader extends ConfigurationLoader {
    /**
     * !!! DO NOT DEFINE A LOGGER here. This class get loaded very early in the process and we don't want to the logger to be initialized yet **
     */
    public static final String ENV_VISALLO_DIR = "VISALLO_DIR";
    public static final String DEFAULT_UNIX_LOCATION = "/opt/openlumify/";
    public static final String DEFAULT_WINDOWS_LOCATION = "c:/opt/openlumify/";

    public static final String ENV_SEARCH_LOCATIONS = "VISALLO_CONFIGURATION_LOADER_SEARCH_LOCATIONS";
    public static final String ENV_SEARCH_LOCATIONS_DEFAULT = Joiner.on(",").join(new String[]{
            SearchLocation.SystemProperty.getValue(),
            SearchLocation.EnvironmentVariable.getValue(),
            SearchLocation.UserHome.getValue(),
            SearchLocation.AppData.getValue(),
            SearchLocation.OpenLumifyDefaultDirectory.getValue()
    });

    public FileConfigurationLoader(Map initParameters) {
        super(initParameters);
    }

    public Configuration createConfiguration() {
        final Map<String, String> properties = getDefaultProperties();
        List<File> configDirectories = getOpenLumifyDirectoriesFromLeastPriority("config");
        if (configDirectories.size() == 0) {
            throw new OpenLumifyException("Could not find any valid config directories.");
        }
        List<String> loadedFiles = new ArrayList<>();
        for (File directory : configDirectories) {
            Map<String, String> directoryProperties = loadDirectory(directory, loadedFiles);
            properties.putAll(directoryProperties);
        }
        setConfigurationInfo("loadedFiles", loadedFiles);
        return new Configuration(this, properties);
    }

    private Map<String, String> getDefaultProperties() {
        Map<String, String> defaultProperties = new HashMap<>(getInitParameters());

        List<File> configDirs = getOpenLumifyDirectoriesFromMostPriority("config");
        if (configDirs.size() > 0) {
            String openlumifyDir = configDirs.get(0).getParentFile().getAbsolutePath();
            defaultProperties.put(ENV_VISALLO_DIR, openlumifyDir);
        }

        return defaultProperties;
    }

    public static List<File> getOpenLumifyDirectoriesFromMostPriority(String subDirectory) {
        List<File> results = new ArrayList<>();

        List<SearchLocation> searchLocations = getSearchLocations();
        for (SearchLocation searchLocation : searchLocations) {
            switch (searchLocation) {
                case AppData:
                    String appData = System.getProperty("appdata");
                    if (appData != null && appData.length() > 0) {
                        addOpenLumifySubDirectory(
                                results,
                                new File(new File(appData), "OpenLumify").getAbsolutePath(),
                                subDirectory
                        );
                    }
                    break;

                case EnvironmentVariable:
                    addOpenLumifySubDirectory(results, System.getenv(ENV_VISALLO_DIR), subDirectory);
                    break;

                case SystemProperty:
                    addOpenLumifySubDirectory(results, System.getProperty(ENV_VISALLO_DIR, null), subDirectory);
                    break;

                case UserHome:
                    String userHome = System.getProperty("user.home");
                    if (userHome != null && userHome.length() > 0) {
                        addOpenLumifySubDirectory(
                                results,
                                new File(new File(userHome), ".openlumify").getAbsolutePath(),
                                subDirectory
                        );
                    }
                    break;

                case OpenLumifyDefaultDirectory:
                    String defaultOpenLumifyDir = getDefaultOpenLumifyDir();
                    addOpenLumifySubDirectory(results, defaultOpenLumifyDir, subDirectory);
                    break;

                default:
                    throw new OpenLumifyException("Unhandled search type: " + searchLocation);
            }
        }

        return ImmutableList.copyOf(results);
    }

    public static List<File> getOpenLumifyDirectoriesFromLeastPriority(String subDirectory) {
        return Lists.reverse(getOpenLumifyDirectoriesFromMostPriority(subDirectory));
    }

    private static List<SearchLocation> getSearchLocations() {
        String locationsString = System.getProperty(ENV_SEARCH_LOCATIONS);
        if (locationsString == null) {
            locationsString = System.getenv(ENV_SEARCH_LOCATIONS);
            if (locationsString == null) {
                locationsString = ENV_SEARCH_LOCATIONS_DEFAULT;
            }
        }

        String[] locationItems = locationsString.split(",");
        List<SearchLocation> searchLocations = new ArrayList<>();
        for (String locationItem : locationItems) {
            searchLocations.add(SearchLocation.parse(locationItem));
        }
        return searchLocations;
    }

    public static String getDefaultOpenLumifyDir() {
        if (ProcessUtil.isWindows()) {
            return DEFAULT_WINDOWS_LOCATION;
        } else {
            return DEFAULT_UNIX_LOCATION;
        }
    }

    private static void addOpenLumifySubDirectory(List<File> results, String location, String subDirectory) {
        if (location == null || location.trim().length() == 0) {
            return;
        }

        location = location.trim();
        if (location.startsWith("file://")) {
            location = location.substring("file://".length());
        }

        File dir = new File(new File(location), subDirectory);
        if (!dir.exists()) {
            return;
        }

        results.add(dir);
    }

    private static Map<String, String> loadDirectory(File configDirectory, List<String> loadedFiles) {
        OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(FileConfigurationLoader.class);

        LOGGER.debug("Attempting to load configuration from directory: %s", configDirectory);
        if (!configDirectory.exists()) {
            throw new OpenLumifyException("Could not find config directory: " + configDirectory);
        }

        File[] files = configDirectory.listFiles();
        if (files == null) {
            throw new OpenLumifyException("Could not parse directory name: " + configDirectory);
        }
        // sort similar to IntelliJ, openlumify.properties should come before openlumify-*.properties
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return getComparableFileName(o1).compareTo(getComparableFileName(o2));
            }

            private String getComparableFileName(File o1) {
                return FilenameUtils.getBaseName(o1.getName()).toLowerCase();
            }
        });
        Map<String, String> properties = new HashMap<>();
        for (File f : files) {
            if (!f.getAbsolutePath().endsWith(".properties")) {
                continue;
            }
            try {
                Map<String, String> fileProperties = loadFile(f.getAbsolutePath(), loadedFiles);
                for (Map.Entry<String, String> filePropertyEntry : fileProperties.entrySet()) {
                    properties.put(filePropertyEntry.getKey(), filePropertyEntry.getValue());
                }
            } catch (IOException ex) {
                throw new OpenLumifyException("Could not load config file: " + f.getAbsolutePath(), ex);
            }
        }

        return properties;
    }

    private static Map<String, String> loadFile(final String fileName, List<String> loadedFiles) throws IOException {
        OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(FileConfigurationLoader.class);

        Map<String, String> results = new HashMap<>();
        LOGGER.info("Loading config file: %s", fileName);
        try (FileInputStream in = new FileInputStream(fileName)) {
            Properties properties = new Properties();
            properties.load(in);
            for (Map.Entry<Object, Object> prop : properties.entrySet()) {
                String key = prop.getKey().toString();
                String value = prop.getValue().toString();
                results.put(key, value);
            }
            loadedFiles.add(fileName);
        } catch (Exception e) {
            LOGGER.info("Could not load configuration file: %s", fileName);
        }
        return results;
    }

    @Override
    public File resolveFileName(String fileName) {
        return resolveLocalFileName(fileName);
    }

    public static File resolveLocalFileName(String fileName) {
        List<File> configDirectories = getOpenLumifyDirectoriesFromMostPriority("config");
        if (configDirectories.size() == 0) {
            throw new OpenLumifyResourceNotFoundException("Could not find any valid config directories.");
        }
        for (File directory : configDirectories) {
            File f = new File(directory, fileName);
            if (f.exists()) {
                return f;
            }
        }
        throw new OpenLumifyResourceNotFoundException("Could not find file: " + fileName);
    }

    public enum SearchLocation {
        OpenLumifyDefaultDirectory("defaultDir"),
        AppData("appdata"),
        UserHome("userHome"),
        EnvironmentVariable("env"),
        SystemProperty("systemProperty");

        private final String value;

        SearchLocation(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return getValue();
        }

        public static SearchLocation parse(String searchType) {
            for (SearchLocation type : SearchLocation.values()) {
                if (type.name().equalsIgnoreCase(searchType) || type.getValue().equalsIgnoreCase(searchType)) {
                    return type;
                }
            }
            throw new OpenLumifyException("Could not parse search type: " + searchType);
        }
    }
}
