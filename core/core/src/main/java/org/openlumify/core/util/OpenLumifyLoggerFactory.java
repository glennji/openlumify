package org.openlumify.core.util;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.slf4j.LoggerFactory;
import org.openlumify.core.config.ConfigurationLoader;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

public class OpenLumifyLoggerFactory {
    private static final Map<String, OpenLumifyLogger> logMap = new HashMap<>();
    private static boolean initialized = false;
    private static boolean initializing = false;

    public static OpenLumifyLogger getLogger(Class clazz, String processType) {
        if (processType != null) {
            setProcessType(processType);
        } else {
            setProcessType(clazz.getSimpleName());
        }
        ensureInitialized();
        return getLogger(clazz.getName());
    }

    public static OpenLumifyLogger getLogger(Class clazz) {
        return getLogger(clazz, null);
    }

    private static void ensureInitialized() {
        synchronized (logMap) {
            if (!initialized && !initializing) {
                initializing = true;
                if (System.getProperty("logFileSuffix") == null) {
                    String hostname = null;
                    try {
                        hostname = InetAddress.getLocalHost().getHostName();
                    } catch (UnknownHostException e) {
                        System.err.println("Could not get host name: " + e.getMessage());
                    }
                    String logFileSuffix = "-" + Joiner.on("-").skipNulls().join(getProcessType(), hostname, ProcessUtil.getPid());
                    System.setProperty("logFileSuffix", logFileSuffix);
                }
                ConfigurationLoader.configureLog4j();
                initialized = true;
                initializing = false;
                logSystem();
            }
        }
    }

    private static void logSystem() {
        OpenLumifyLogger logger = getLogger(OpenLumifyLoggerFactory.class);
        logEnv(logger);
        logSystemProperties(logger);
        logJvmInputArguments(logger);
    }

    private static void logJvmInputArguments(OpenLumifyLogger logger) {
        logger.info("jvm input arguments:");
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        List<String> arguments = runtimeMxBean.getInputArguments();
        for (String arg : arguments) {
            logger.info("  %s", arg);
        }
    }

    private static void logSystemProperties(OpenLumifyLogger logger) {
        logger.info("system properties:");
        ArrayList<Map.Entry<Object, Object>> properties = Lists.newArrayList(System.getProperties().entrySet());
        Collections.sort(properties, new Comparator<Map.Entry<Object, Object>>() {
            @Override
            public int compare(Map.Entry<Object, Object> o1, Map.Entry<Object, Object> o2) {
                return o1.getKey().toString().compareTo(o2.getKey().toString());
            }
        });
        for (final Map.Entry<Object, Object> entry : properties) {
            logger.info("  %s: %s", entry.getKey(), entry.getValue());
        }
    }

    private static void logEnv(OpenLumifyLogger logger) {
        logger.info("environment:");
        ArrayList<Map.Entry<String, String>> entries = Lists.newArrayList(System.getenv().entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<String, String>>() {
            @Override
            public int compare(Map.Entry<String, String> o1, Map.Entry<String, String> o2) {
                return o1.getKey().compareTo(o2.getKey());
            }
        });
        for (final Map.Entry<String, String> entry : entries) {
            logger.info("  %s: %s", entry.getKey(), entry.getValue());
        }
    }

    public static void setProcessType(String processType) {
        if (getProcessType() == null) {
            if (initializing) {
                System.err.println("setProcessType called too late");
            } else if (initialized) {
                getLogger(OpenLumifyLoggerFactory.class).warn("setProcessType called too late");
            }
            System.setProperty("openlumify.processType", processType);
        }
    }

    private static String getProcessType() {
        return System.getProperty("openlumify.processType");
    }

    public static OpenLumifyLogger getLogger(String name) {
        ensureInitialized();
        synchronized (logMap) {
            OpenLumifyLogger openlumifyLogger = logMap.get(name);
            if (openlumifyLogger != null) {
                return openlumifyLogger;
            }
            openlumifyLogger = new OpenLumifyLogger(LoggerFactory.getLogger(name));
            logMap.put(name, openlumifyLogger);
            return openlumifyLogger;
        }
    }
}
