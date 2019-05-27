package org.openlumify.core.model.longRunningProcess;

import org.openlumify.core.model.properties.types.JsonSingleValueOpenLumifyProperty;

public class LongRunningProcessProperties {
    public static final String LONG_RUNNING_PROCESS_CONCEPT_IRI = "http://openlumify.org/longRunningProcess#longRunningProcess";
    public static final String LONG_RUNNING_PROCESS_TO_USER_EDGE_IRI = "http://openlumify.org/longRunningProcess#hasLongRunningProcess";
    public static final String LONG_RUNNING_PROCESS_ID_PREFIX = "LONG_RUNNING_PROCESS_";
    public static final String OWL_IRI = "http://openlumify.org/longRunningProcess";

    public static JsonSingleValueOpenLumifyProperty QUEUE_ITEM_JSON_PROPERTY = new JsonSingleValueOpenLumifyProperty("http://openlumify.org/longRunningProcess#queueItemJson");
}
