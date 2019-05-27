package org.openlumify.core.config;

import org.openlumify.core.exception.OpenLumifyException;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Map;
import java.util.Properties;

public class HashMapConfigurationLoader extends ConfigurationLoader {
    public HashMapConfigurationLoader(String propertiesString) {
        this(parseStringToProperties(propertiesString));
    }

    public HashMapConfigurationLoader(Map initParameters) {
        super(initParameters);
    }

    @Override
    public Configuration createConfiguration() {
        return new Configuration(this, getInitParameters());
    }

    @Override
    public File resolveFileName(String fileName) {
        return FileConfigurationLoader.resolveLocalFileName(fileName);
    }

    private static Properties parseStringToProperties(String propertiesString) {
        Properties properties = new Properties();
        Reader reader = new StringReader(propertiesString);
        try {
            properties.load(reader);
        } catch (IOException ex) {
            throw new OpenLumifyException("Could not load properties string", ex);
        }
        return properties;
    }
}
