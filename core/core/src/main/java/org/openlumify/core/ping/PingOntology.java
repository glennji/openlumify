package org.openlumify.core.ping;

import com.google.common.base.Strings;
import org.openlumify.core.model.properties.types.DateSingleValueOpenLumifyProperty;
import org.openlumify.core.model.properties.types.LongSingleValueOpenLumifyProperty;
import org.openlumify.core.model.properties.types.StringSingleValueOpenLumifyProperty;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PingOntology {
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private static final String HOST_NAME = getHostName();

    public static final String BASE_IRI = "http://openlumify.org/ping";

    public static final String IRI_CONCEPT_PING = BASE_IRI + "#ping";

    public static final DateSingleValueOpenLumifyProperty CREATE_DATE = new DateSingleValueOpenLumifyProperty(BASE_IRI + "#createDate");
    public static final StringSingleValueOpenLumifyProperty CREATE_REMOTE_ADDR = new StringSingleValueOpenLumifyProperty(BASE_IRI + "#createRemoteAddr");
    public static final LongSingleValueOpenLumifyProperty SEARCH_TIME_MS = new LongSingleValueOpenLumifyProperty(BASE_IRI + "#searchTimeMs");
    public static final LongSingleValueOpenLumifyProperty RETRIEVAL_TIME_MS = new LongSingleValueOpenLumifyProperty(BASE_IRI + "#retrievalTimeMs");
    public static final DateSingleValueOpenLumifyProperty GRAPH_PROPERTY_WORKER_DATE = new DateSingleValueOpenLumifyProperty(BASE_IRI + "#gpwDate");
    public static final StringSingleValueOpenLumifyProperty GRAPH_PROPERTY_WORKER_HOSTNAME = new StringSingleValueOpenLumifyProperty(BASE_IRI + "#gpwHostname");
    public static final StringSingleValueOpenLumifyProperty GRAPH_PROPERTY_WORKER_HOST_ADDRESS = new StringSingleValueOpenLumifyProperty(BASE_IRI + "#gpwHostAddress");
    public static final LongSingleValueOpenLumifyProperty GRAPH_PROPERTY_WORKER_WAIT_TIME_MS = new LongSingleValueOpenLumifyProperty(BASE_IRI + "#gpwWaitTimeMs");
    public static final DateSingleValueOpenLumifyProperty LONG_RUNNING_PROCESS_DATE = new DateSingleValueOpenLumifyProperty(BASE_IRI + "#lrpDate");
    public static final StringSingleValueOpenLumifyProperty LONG_RUNNING_PROCESS_HOSTNAME = new StringSingleValueOpenLumifyProperty(BASE_IRI + "#lrpHostname");
    public static final StringSingleValueOpenLumifyProperty LONG_RUNNING_PROCESS_HOST_ADDRESS = new StringSingleValueOpenLumifyProperty(BASE_IRI + "#lrpHostAddress");
    public static final LongSingleValueOpenLumifyProperty LONG_RUNNING_PROCESS_WAIT_TIME_MS = new LongSingleValueOpenLumifyProperty(BASE_IRI + "#lrpWaitTimeMs");

    public static String getVertexId(Date date) {
        return "PING_" + new SimpleDateFormat(DATE_TIME_FORMAT).format(date) + "_" + HOST_NAME;
    }

    private static String getHostName() {
        // Windows
        String host = System.getenv("COMPUTERNAME");
        if (!Strings.isNullOrEmpty(host)) {
            return host;
        }

        // Unix'ish
        host = System.getenv("HOSTNAME");
        if (!Strings.isNullOrEmpty(host)) {
            return host;
        }

        // Java which requires DNS resolution
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException uhe) {
            return "Unknown";
        }
    }
}
