package org.openlumify.core.ingest.video;

import org.openlumify.core.model.properties.OpenLumifyProperties;
import org.openlumify.core.model.properties.MediaOpenLumifyProperties;
import org.openlumify.core.util.RowKeyHelper;
import org.vertexium.Property;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VideoPropertyHelper {
    private static final Pattern START_TIME_AND_END_TIME_PATTERN = Pattern.compile("^(.*)" + RowKeyHelper.FIELD_SEPARATOR + MediaOpenLumifyProperties.VIDEO_FRAME.getPropertyName() + RowKeyHelper.FIELD_SEPARATOR + "([0-9]+)" + RowKeyHelper.FIELD_SEPARATOR + "([0-9]+)");
    private static final Pattern START_TIME_ONLY_PATTERN = Pattern.compile("^(.*)" + RowKeyHelper.FIELD_SEPARATOR + MediaOpenLumifyProperties.VIDEO_FRAME.getPropertyName() + RowKeyHelper.FIELD_SEPARATOR + "([0-9]+)");

    public static VideoFrameInfo getVideoFrameInfoFromProperty(Property property) {
        String mimeType = OpenLumifyProperties.MIME_TYPE_METADATA.getMetadataValueOrDefault(property.getMetadata(), null);
        if (mimeType == null || !mimeType.equals("text/plain")) {
            return null;
        }
        return getVideoFrameInfo(property.getKey());
    }

    public static VideoFrameInfo getVideoFrameInfo(String propertyKey) {
        Matcher m = START_TIME_AND_END_TIME_PATTERN.matcher(propertyKey);
        if (m.find()) {
            return new VideoFrameInfo(Long.parseLong(m.group(2)), Long.parseLong(m.group(3)), m.group(1));
        }

        m = START_TIME_ONLY_PATTERN.matcher(propertyKey);
        if (m.find()) {
            return new VideoFrameInfo(Long.parseLong(m.group(2)), null, m.group(1));
        }
        return null;
    }
}
