package org.openlumify.web.routes.vertex;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Optional;
import org.visallo.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.vertexium.property.StreamingPropertyValue;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.model.properties.MediaOpenLumifyProperties;
import org.openlumify.core.model.properties.OpenLumifyProperties;
import org.openlumify.core.util.LimitInputStream;
import org.openlumify.web.BadRequestException;
import org.openlumify.web.OpenLumifyResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class VertexRaw implements ParameterizedHandler {
    private static final Pattern RANGE_PATTERN = Pattern.compile("bytes=([0-9]*)-([0-9]*)");

    private final Graph graph;

    @Inject
    public VertexRaw(final Graph graph) {
        this.graph = graph;
    }

    @Handle
    public InputStream handle(
            HttpServletRequest request,
            @Required(name = "graphVertexId") String graphVertexId,
            @Optional(name = "download", defaultValue = "false") boolean download,
            @Optional(name = "playback", defaultValue = "false") boolean playback,
            @Optional(name = "type") String type,
            Authorizations authorizations,
            OpenLumifyResponse response
    ) throws Exception {
        Vertex artifactVertex = graph.getVertex(graphVertexId, authorizations);
        if (artifactVertex == null) {
            throw new OpenLumifyResourceNotFoundException("Could not find vertex with id: " + graphVertexId);
        }

        String fileName = OpenLumifyProperties.FILE_NAME.getOnlyPropertyValue(artifactVertex);

        if (playback) {
            return handlePartialPlayback(request, response, artifactVertex, fileName, type);
        } else {
            String mimeType = getMimeType(artifactVertex);
            response.setContentType(mimeType);
            response.setMaxAge(OpenLumifyResponse.EXPIRES_1_HOUR);
            if (fileName == null) {
                throw new OpenLumifyResourceNotFoundException("Could not find fileName on artifact: " + artifactVertex.getId());
            }
            String fileNameWithoutQuotes = fileName.replace('"', '\'');
            if (download) {
                response.addHeader("Content-Disposition", "attachment; filename=\"" + fileNameWithoutQuotes + "\"");
            } else {
                response.addHeader("Content-Disposition", "inline; filename=\"" + fileNameWithoutQuotes + "\"");
            }

            StreamingPropertyValue rawValue = OpenLumifyProperties.RAW.getPropertyValue(artifactVertex);
            if (rawValue == null) {
                throw new OpenLumifyResourceNotFoundException("Could not find raw on artifact: " + artifactVertex.getId());
            }
            return rawValue.getInputStream();
        }
    }

    private InputStream handlePartialPlayback(HttpServletRequest request, OpenLumifyResponse response, Vertex artifactVertex, String fileName, String type) throws IOException {
        if (type == null) {
            throw new BadRequestException("type is required for partial playback");
        }

        InputStream in;
        Long totalLength;
        long partialStart = 0;
        Long partialEnd = null;
        String range = request.getHeader("Range");

        if (range != null) {
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);

            Matcher m = RANGE_PATTERN.matcher(range);
            if (m.matches()) {
                partialStart = Long.parseLong(m.group(1));
                if (m.group(2).length() > 0) {
                    partialEnd = Long.parseLong(m.group(2));
                }
            }
        }

        response.setCharacterEncoding(null);
        response.setContentType(type);
        response.addHeader("Content-Disposition", "attachment; filename=" + fileName);

        StreamingPropertyValue mediaPropertyValue = getStreamingPropertyValue(artifactVertex, type);

        totalLength = mediaPropertyValue.getLength();
        in = mediaPropertyValue.getInputStream();

        if (partialEnd == null) {
            partialEnd = totalLength;
        }

        // Ensure that the last byte position is less than the instance-length
        partialEnd = Math.min(partialEnd, totalLength - 1);
        long partialLength = totalLength;

        if (range != null) {
            partialLength = partialEnd - partialStart + 1;
            response.addHeader("Content-Range", "bytes " + partialStart + "-" + partialEnd + "/" + totalLength);
            if (partialStart > 0) {
                in.skip(partialStart);
            }
        }

        response.addHeader("Content-Length", "" + partialLength);

        return new LimitInputStream(in, partialLength);
    }

    private StreamingPropertyValue getStreamingPropertyValue(Vertex artifactVertex, String type) {
        StreamingPropertyValue mediaPropertyValue;
        if (MediaOpenLumifyProperties.MIME_TYPE_AUDIO_MP4.equals(type)) {
            mediaPropertyValue = MediaOpenLumifyProperties.AUDIO_MP4.getPropertyValue(artifactVertex);
            checkNotNull(mediaPropertyValue, String.format("Could not find %s property on artifact %s", MediaOpenLumifyProperties.MIME_TYPE_AUDIO_MP4, artifactVertex.getId()));
        } else if (MediaOpenLumifyProperties.MIME_TYPE_AUDIO_OGG.equals(type)) {
            mediaPropertyValue = MediaOpenLumifyProperties.AUDIO_OGG.getPropertyValue(artifactVertex);
            checkNotNull(mediaPropertyValue, String.format("Could not find %s property on artifact %s", MediaOpenLumifyProperties.MIME_TYPE_AUDIO_OGG, artifactVertex.getId()));
        } else if (MediaOpenLumifyProperties.MIME_TYPE_VIDEO_MP4.equals(type)) {
            mediaPropertyValue = MediaOpenLumifyProperties.VIDEO_MP4.getPropertyValue(artifactVertex);
            checkNotNull(mediaPropertyValue, String.format("Could not find %s property on artifact %s", MediaOpenLumifyProperties.MIME_TYPE_VIDEO_MP4, artifactVertex.getId()));
        } else if (MediaOpenLumifyProperties.MIME_TYPE_VIDEO_WEBM.equals(type)) {
            mediaPropertyValue = MediaOpenLumifyProperties.VIDEO_WEBM.getPropertyValue(artifactVertex);
            checkNotNull(mediaPropertyValue, String.format("Could not find %s property on artifact %s", MediaOpenLumifyProperties.MIME_TYPE_VIDEO_WEBM, artifactVertex.getId()));
        } else {
            throw new OpenLumifyException("Invalid video type: " + type);
        }
        return mediaPropertyValue;
    }

    private void copy(InputStream in, OutputStream out, Long length) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while (length > 0 && (read = in.read(buffer, 0, (int) Math.min(length, buffer.length))) > 0) {
            out.write(buffer, 0, read);
            length -= read;
        }
    }

    private String getMimeType(Vertex artifactVertex) {
        String mimeType = OpenLumifyProperties.MIME_TYPE.getOnlyPropertyValue(artifactVertex);
        if (mimeType == null || mimeType.isEmpty()) {
            mimeType = "application/octet-stream";
        }
        return mimeType;
    }
}
