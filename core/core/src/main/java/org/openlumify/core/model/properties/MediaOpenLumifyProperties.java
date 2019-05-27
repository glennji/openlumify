package org.openlumify.core.model.properties;

import org.openlumify.core.model.properties.types.StreamingOpenLumifyProperty;
import org.openlumify.core.model.properties.types.StreamingSingleValueOpenLumifyProperty;
import org.openlumify.core.model.properties.types.VideoTranscriptProperty;

/**
 * OpenLumifyProperties for media files (video, images, etc.).
 */
public class MediaOpenLumifyProperties {
    public static final String MIME_TYPE_VIDEO_MP4 = "video/mp4";
    public static final String MIME_TYPE_VIDEO_WEBM = "video/webm";
    public static final String MIME_TYPE_AUDIO_MP3 = "audio/mp3";
    public static final String MIME_TYPE_AUDIO_MP4 = "audio/mp4";
    public static final String MIME_TYPE_AUDIO_OGG = "audio/ogg";

    public static final String METADATA_VIDEO_FRAME_START_TIME = "http://openlumify.org#videoFrameStartTime";

    public static final StreamingSingleValueOpenLumifyProperty VIDEO_MP4 = new StreamingSingleValueOpenLumifyProperty("http://openlumify.org#video-mp4");
    public static final StreamingSingleValueOpenLumifyProperty VIDEO_WEBM = new StreamingSingleValueOpenLumifyProperty("http://openlumify.org#video-webm");
    public static final StreamingSingleValueOpenLumifyProperty AUDIO_MP3 = new StreamingSingleValueOpenLumifyProperty("http://openlumify.org#audio-mp3");
    public static final StreamingSingleValueOpenLumifyProperty AUDIO_MP4 = new StreamingSingleValueOpenLumifyProperty("http://openlumify.org#audio-mp4");
    public static final StreamingSingleValueOpenLumifyProperty AUDIO_OGG = new StreamingSingleValueOpenLumifyProperty("http://openlumify.org#audio-ogg");

    public static final VideoTranscriptProperty VIDEO_TRANSCRIPT = new VideoTranscriptProperty("http://openlumify.org#videoTranscript");
    public static final StreamingOpenLumifyProperty RAW_POSTER_FRAME = new StreamingOpenLumifyProperty("http://openlumify.org#rawPosterFrame");
    public static final StreamingSingleValueOpenLumifyProperty VIDEO_PREVIEW_IMAGE = new StreamingSingleValueOpenLumifyProperty("http://openlumify.org#videoPreviewImage");
    public static final StreamingOpenLumifyProperty VIDEO_FRAME = new StreamingOpenLumifyProperty("http://openlumify.org#videoFrame");

    private MediaOpenLumifyProperties() {
        throw new UnsupportedOperationException("do not construct utility class");
    }
}
