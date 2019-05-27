package org.openlumify.web.routes.vertex;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.webster.ParameterizedHandler;
import org.openlumify.webster.annotations.Handle;
import org.openlumify.webster.annotations.Optional;
import org.openlumify.webster.annotations.Required;
import org.apache.commons.io.IOUtils;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.vertexium.property.StreamingPropertyValue;
import org.openlumify.core.config.Configuration;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.model.thumbnails.ThumbnailRepository;
import org.openlumify.core.user.User;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;
import org.openlumify.web.OpenLumifyResponse;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;

import java.io.InputStream;
import java.io.OutputStream;

import static org.openlumify.core.model.properties.MediaOpenLumifyProperties.VIDEO_PREVIEW_IMAGE;

@Singleton
public class VertexVideoPreviewImage implements ParameterizedHandler {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(VertexVideoPreviewImage.class);
    private final Graph graph;
    private final ThumbnailRepository thumbnailRepository;
    private int framesPerPreview;

    @Inject
    public VertexVideoPreviewImage(
            final Graph graph,
            final ThumbnailRepository thumbnailRepository,
            final Configuration configuration
    ) {
        this.graph = graph;
        this.thumbnailRepository = thumbnailRepository;
        framesPerPreview = configuration.getInt(Configuration.VIDEO_PREVIEW_FRAMES_COUNT, ThumbnailRepository.DEFAULT_FRAMES_PER_PREVIEW);
    }

    @Handle
    public void handle(
            @Required(name = "graphVertexId") String graphVertexId,
            @Optional(name = "width") Integer width,
            @ActiveWorkspaceId String workspaceId,
            User user,
            Authorizations authorizations,
            OpenLumifyResponse response
    ) throws Exception {
        Vertex artifactVertex = graph.getVertex(graphVertexId, authorizations);
        if (artifactVertex == null) {
            throw new OpenLumifyResourceNotFoundException("Could not find vertex with id: " + graphVertexId);
        }

        int[] boundaryDims = new int[]{200 * framesPerPreview, 200};

        if (width != null) {
            boundaryDims[0] = width * framesPerPreview;
            boundaryDims[1] = width;

            response.setContentType("image/jpeg");
            response.addHeader("Content-Disposition", "inline; filename=videoPreview" + boundaryDims[0] + ".jpg");
            response.setMaxAge(OpenLumifyResponse.EXPIRES_1_HOUR);

            byte[] thumbnailData = thumbnailRepository.getThumbnailData(
                    artifactVertex.getId(),
                    "video-preview",
                    boundaryDims[0],
                    boundaryDims[1],
                    workspaceId,
                    user);
            if (thumbnailData != null) {
                LOGGER.debug("Cache hit for: %s (video-preview) %d x %d", artifactVertex.getId(), boundaryDims[0], boundaryDims[1]);
                try (OutputStream out = response.getOutputStream()) {
                    out.write(thumbnailData);
                }
                return;
            }
        }

        Property videoPreviewImage = VIDEO_PREVIEW_IMAGE.getProperty(artifactVertex);
        StreamingPropertyValue videoPreviewImageValue = VIDEO_PREVIEW_IMAGE.getPropertyValue(artifactVertex);
        if (videoPreviewImageValue == null) {
            LOGGER.warn("Could not find video preview image for artifact: %s", artifactVertex.getId());
            response.respondWithNotFound();
            return;
        }
        try (InputStream in = videoPreviewImageValue.getInputStream()) {
            if (width != null) {
                LOGGER.info("Cache miss for: %s (video-preview) %d x %d", artifactVertex.getId(), boundaryDims[0], boundaryDims[1]);

                response.setContentType("image/jpeg");
                response.addHeader("Content-Disposition", "inline; filename=videoPreview" + boundaryDims[0] + ".jpg");
                response.setMaxAge(OpenLumifyResponse.EXPIRES_1_HOUR);

                byte[] thumbnailData = thumbnailRepository.createThumbnail(artifactVertex, videoPreviewImage.getKey(), "video-preview", in, boundaryDims, user).getData();
                try (OutputStream out = response.getOutputStream()) {
                    out.write(thumbnailData);
                }
            } else {
                response.setContentType("image/png");
                try (OutputStream out = response.getOutputStream()) {
                    IOUtils.copy(in, out);
                }
            }
        }
    }
}
