package org.openlumify.core.trace;

import org.openlumify.core.bootstrap.InjectHelper;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Trace.on("start some process");
 * ...
 * TraceSpan trace = Trace.start("subprocess");
 * trace.data("data", "some data");
 * try {
 * ...
 * } finally {
 * trace.close();
 * }
 * ...
 * Trace.off();
 */
public class Trace {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(Trace.class);
    private static TraceRepository traceRepository;

    public static void on(String description) {
        getTraceRepository().on(description, new HashMap<>());
    }

    public static TraceSpan on(String description, Map<String, String> data) {
        return getTraceRepository().on(description, data);
    }

    public static void off() {
        getTraceRepository().off();
    }

    public static TraceSpan start(String description) {
        return getTraceRepository().start(description);
    }

    public static boolean isEnabled() {
        return getTraceRepository().isEnabled();
    }

    private static TraceRepository getTraceRepository() {
        if (traceRepository == null) {
            try {
                traceRepository = InjectHelper.getInstance(TraceRepository.class);
            } catch (OpenLumifyException e) {
                LOGGER.warn("TraceRepository not found through injection. Using no-op DefaultTraceRepository");
                traceRepository = new DefaultTraceRepository();
            }
        }
        return traceRepository;
    }
}
