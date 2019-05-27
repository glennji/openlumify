package org.openlumify.tikaMimeType;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

import java.io.BufferedInputStream;
import java.io.InputStream;

public class TikaMimeTypeMapper {
    private final OpenLumifyMimeTypeDetector detector;

    public TikaMimeTypeMapper() {
        detector = new OpenLumifyMimeTypeDetector();
    }

    public String guessMimeType(InputStream in, String fileName) throws Exception {
        Metadata metadata = new Metadata();
        metadata.set(OpenLumifyMimeTypeDetector.METADATA_FILENAME, fileName);
        MediaType mediaType = detector.detect(new BufferedInputStream(in), metadata);
        String mimeType = mediaType.toString();
        if (mimeType != null) {
            return mimeType;
        }

        return "application/octet-stream";
    }
}
