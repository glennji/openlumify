package org.openlumify.web.routes.resource;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Optional;
import org.visallo.webster.annotations.Required;
import org.apache.commons.io.IOUtils;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.vertexium.mutation.ExistingElementMutation;
import org.vertexium.property.StreamingPropertyValue;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.http.HttpRepository;
import org.openlumify.core.model.properties.OpenLumifyProperties;
import org.openlumify.core.util.ImageUtils;
import org.openlumify.web.OpenLumifyResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Singleton
public class ResourceExternalGet implements ParameterizedHandler {
    private final Graph graph;
    private final HttpRepository httpRepository;

    @Inject
    public ResourceExternalGet(
            final Graph graph,
            final HttpRepository httpRepository
    ) {
        this.graph = graph;
        this.httpRepository = httpRepository;
    }

    @Handle
    public void handle(
            Authorizations authorizations,
            @Required(name = "vId") String vertexId,
            @Required(name = "url") String url,
            @Required(name = "maxWidth") int maxWidth,
            @Required(name = "maxHeight") int maxHeight,
            @Optional(name = "jpegQuality", defaultValue = "80") int jpegQuality,
            OpenLumifyResponse response
    ) throws Exception {
        String propertyKey = getPropertyKey(url, maxWidth, maxHeight, jpegQuality);
        Vertex vertex = this.graph.getVertex(vertexId, authorizations);
        if (vertex == null) {
            throw new OpenLumifyResourceNotFoundException("Could not find vertex: " + vertexId);
        }

        InputStream in;
        StreamingPropertyValue cachedImageValue = OpenLumifyProperties.CACHED_IMAGE.getPropertyValue(vertex, propertyKey);
        if (cachedImageValue != null) {
            in = cachedImageValue.getInputStream();
        } else {
            byte[] imageData = createAndSaveCachedImage(vertex, propertyKey, url, maxWidth, maxHeight, jpegQuality, authorizations);
            in = new ByteArrayInputStream(imageData);
        }

        ImageUtils.ImageFormat imageFormat = ImageUtils.getImageFormat(in);
        String imageMimeType = imageFormat.getImageMimeType();
        if (imageMimeType == null) {
            imageMimeType = "image";
        }

        response.setContentType(imageMimeType);
        response.addHeader("Content-Disposition", "inline; filename=thumbnail-" + maxWidth + "x" + maxHeight + ".jpg");
        response.setMaxAge(OpenLumifyResponse.EXPIRES_1_HOUR);

        response.write(imageFormat.getPushBackIn());
    }

    private byte[] createAndSaveCachedImage(Vertex vertex, String propertyKey, String url, int maxWidth, int maxHeight, int jpegQuality, Authorizations authorizations) throws IOException {
        byte[] imageData = getAndSaveImageData(vertex, url, authorizations);
        imageData = ImageUtils.resize(imageData, maxWidth, maxHeight, jpegQuality);

        StreamingPropertyValue value = StreamingPropertyValue.create(new ByteArrayInputStream(imageData), byte[].class);
        value.store(true).searchIndex(false);
        ExistingElementMutation<Vertex> m = vertex.prepareMutation();
        OpenLumifyProperties.CACHED_IMAGE.addPropertyValue(m, propertyKey, value, vertex.getVisibility());
        m.save(authorizations);
        return imageData;
    }

    private byte[] getAndSaveImageData(Vertex vertex, String url, Authorizations authorizations) throws IOException {
        String propertyKey = getPropertyKey(url, null, null, null);
        StreamingPropertyValue originalImage = OpenLumifyProperties.CACHED_IMAGE.getPropertyValue(vertex, propertyKey);
        if (originalImage != null) {
            return IOUtils.toByteArray(originalImage.getInputStream());
        }
        byte[] imageData = httpRepository.get(url);
        StreamingPropertyValue value = StreamingPropertyValue.create(new ByteArrayInputStream(imageData), byte[].class);
        value.store(true).searchIndex(false);
        ExistingElementMutation<Vertex> m = vertex.prepareMutation();
        OpenLumifyProperties.CACHED_IMAGE.addPropertyValue(m, propertyKey, value, vertex.getVisibility());
        m.save(authorizations);
        return imageData;
    }

    private String getPropertyKey(String url, Integer maxWidth, Integer maxHeight, Integer jpegQuality) {
        String result = url;
        if (maxWidth != null) {
            result += "-" + Integer.toString(maxWidth);
        }
        if (maxHeight != null) {
            result += "-" + Integer.toString(maxHeight);
        }
        if (jpegQuality != null) {
            result += "-" + Integer.toString(jpegQuality);
        }
        return result;
    }
}

