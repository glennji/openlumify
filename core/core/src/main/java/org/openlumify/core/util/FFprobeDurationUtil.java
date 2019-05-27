package org.openlumify.core.util;

import org.json.JSONObject;

public class FFprobeDurationUtil {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(FFprobeDurationUtil.class);

    public static Double getDuration(JSONObject json) {
        if (json == null) {
            return null;
        }

        JSONObject formatJson = json.optJSONObject("format");
        if (formatJson != null) {
            Double duration = formatJson.optDouble("duration");
            if (!Double.isNaN(duration)) {
                return duration;
            }
        }

        LOGGER.debug("Could not retrieve a \"duration\" value from the JSON object.");
        return null;
    }
}
