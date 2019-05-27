package org.openlumify.core.config;

import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenLumifyResourceBundle extends ResourceBundle {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(OpenLumifyResourceBundle.class);
    private static final Pattern ALIAS_KEY_PATTERN = Pattern.compile("@\\{([\\w\\.]+)\\}");
    private Properties properties;
    private ResourceBundle rootResourceBundle;
    private Properties aliasedProperties = new Properties();

    public OpenLumifyResourceBundle(Properties properties, ResourceBundle rootResourceBundle) {
        this.properties = properties;
        this.rootResourceBundle = rootResourceBundle;
        this.aliasedProperties = getAliasProperties();

    }

    @Override
    protected Object handleGetObject(String key) {
        String value = aliasedProperties.getProperty(key);
        if (value != null) {
            return value;
        }
        value = properties.getProperty(key);
        if (value != null) {
            return value;
        }
        return rootResourceBundle.getString(key);
    }

    @Override
    public Enumeration<String> getKeys() {
        Set keys = new HashSet();
        keys.addAll(aliasedProperties.keySet());
        keys.addAll(properties.keySet());
        keys.addAll(rootResourceBundle.keySet());
        return Collections.enumeration(keys);
    }

    protected Properties getAliasProperties() {
        Properties properties = new Properties();
        Enumeration<String> keys = getKeys();

        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            String value = handleGetObject(key).toString();
            Matcher m = ALIAS_KEY_PATTERN.matcher(value);

            while (m.find(0)) {
                String aliasKey = m.group(1);

                try {
                    aliasKey = (String) handleGetObject(aliasKey);
                } catch (MissingResourceException ex) {
                    LOGGER.debug("No key for alias: %s", aliasKey);
                } catch (Exception ex) {
                    throw ex;
                }

                String aliasedValue = m.replaceFirst(aliasKey);
                properties.setProperty(key, aliasedValue);

                m = ALIAS_KEY_PATTERN.matcher(aliasedValue);
            }
        }

        return properties;
    }
}
