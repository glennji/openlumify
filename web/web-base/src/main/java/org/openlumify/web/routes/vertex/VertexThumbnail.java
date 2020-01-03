package org.openlumify.web.routes.vertex;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Optional;
import org.visallo.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.vertexium.property.StreamingPropertyValue;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.model.properties.OpenLumifyProperties;
import org.openlumify.core.model.thumbnails.Thumbnail;
import org.openlumify.core.model.thumbnails.ThumbnailRepository;
import org.openlumify.core.user.User;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;
import org.openlumify.web.OpenLumifyResponse;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;

import java.io.InputStream;
import java.io.OutputStream;

@Singleton
public class VertexThumbnail implements ParameterizedHandler {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(VertexThumbnail.class);

    private final ThumbnailRepository thumbnailRepository;
    private final Graph graph;

    @Inject
    public VertexThumbnail(
            final ThumbnailRepository thumbnailRepository,
            final Graph graph
    ) {
        this.thumbnailRepository = thumbnailRepository;
        this.graph = graph;
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

        int[] boundaryDims = new int[]{200, 200};
        if (width != null) {
            boundaryDims[0] = boundaryDims[1] = width;
        }

        byte[] thumbnailData;
        Thumbnail thumbnail = thumbnailRepository.getThumbnail(
                artifactVertex.getId(),
                "raw",
                boundaryDims[0], boundaryDims[1],
                workspaceId,
                user);
        if (thumbnail != null) {
            String format = thumbnail.getFormat();
            response.setContentType("image/" + format);
            response.addHeader("Content-Disposition", "inline; filename=thumbnail" + boundaryDims[0] + "." + format);
            response.setMaxAge(OpenLumifyResponse.EXPIRES_1_HOUR);

            thumbnailData = thumbnail.getData();
            if (thumbnailData != null) {
                LOGGER.debug("Cache hit for: %s (raw) %d x %d", artifactVertex.getId(), boundaryDims[0], boundaryDims[1]);
                try (OutputStream out = response.getOutputStream()) {
                    out.write(thumbnailData);
                }
                return;
            }
        }

        LOGGER.info("Cache miss for: %s (raw) %d x %d", artifactVertex.getId(), boundaryDims[0], boundaryDims[1]);
        Property rawProperty = OpenLumifyProperties.RAW.getProperty(artifactVertex);
        StreamingPropertyValue rawPropertyValue = OpenLumifyProperties.RAW.getPropertyValue(artifactVertex);
        if (rawPropertyValue == null) {
            throw new OpenLumifyResourceNotFoundException("Could not find raw property on vertex: " + artifactVertex.getId());
        }

        try (InputStream in = rawPropertyValue.getInputStream()) {
            thumbnail = thumbnailRepository.createThumbnail(artifactVertex, rawProperty.getKey(), "raw", in, boundaryDims, user);

            String format = thumbnail.getFormat();
            response.setContentType("image/" + format);
            response.addHeader("Content-Disposition", "inline; filename=thumbnail" + boundaryDims[0] + "." + format);
            response.setMaxAge(OpenLumifyResponse.EXPIRES_1_HOUR);

            thumbnailData = thumbnail.getData();
        }
        try (OutputStream out = response.getOutputStream()) {
            out.write(thumbnailData);
        }
    }
}
