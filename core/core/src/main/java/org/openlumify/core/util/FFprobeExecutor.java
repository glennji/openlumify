package org.openlumify.core.util;

import org.openlumify.core.exception.OpenLumifyJsonParseException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

public class FFprobeExecutor {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(FFprobeExecutor.class);

    public static JSONObject getJson(ProcessRunner processRunner, String absolutePath) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        String output = null;
        try {
            processRunner.execute(
                    "ffprobe",
                    new String[]{
                            "-v", "quiet",
                            "-print_format", "json",
                            "-show_format",
                            "-show_streams",
                            absolutePath
                    },
                    byteArrayOutputStream,
                    absolutePath + ": "
            );
            output = new String(byteArrayOutputStream.toByteArray());
            return JSONUtil.parse(output);
        } catch (OpenLumifyJsonParseException e) {
            LOGGER.error("unable to parse ffprobe output: [%s]", output);
        } catch (Exception e) {
            LOGGER.error("exception running ffprobe", e);
        }

        return null;
    }
}