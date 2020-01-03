package org.openlumify.web.routes.resource;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Optional;
import org.visallo.webster.annotations.Required;
import org.openlumify.core.model.ontology.Concept;
import org.openlumify.core.model.ontology.OntologyRepository;
import org.openlumify.core.model.thumbnails.ThumbnailRepository;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;
import org.openlumify.web.OpenLumifyResponse;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class MapMarkerImage implements ParameterizedHandler {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(MapMarkerImage.class);

    private final OntologyRepository ontologyRepository;
    private ThumbnailRepository thumbnailRepository;
    private final Cache<String, byte[]> imageCache = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    @Inject
    public MapMarkerImage(
            final OntologyRepository ontologyRepository,
            final ThumbnailRepository thumbnailRepository
    ) {
        this.ontologyRepository = ontologyRepository;
        this.thumbnailRepository = thumbnailRepository;
    }

    @Handle
    public void handle(
            @Required(name = "type") String typeStr,
            @Optional(name = "scale", defaultValue = "1") long scale,
            @Optional(name = "heading", defaultValue = "0.0") double headingParam,
            @Optional(name = "selected", defaultValue = "false") boolean selected,
            OpenLumifyResponse response,
            @ActiveWorkspaceId String workspaceId
    ) throws Exception {
        int heading = roundHeadingAngle(headingParam);
        typeStr = typeStr.isEmpty() ? "http://www.w3.org/2002/07/owl#Thing" : typeStr;
        String cacheKey = typeStr + scale + heading + (selected ? "selected" : "unselected");
        byte[] imageData = imageCache.getIfPresent(cacheKey);
        if (imageData == null) {
            LOGGER.info("map marker cache miss %s (scale: %d, heading: %d)", typeStr, scale, heading);

            Concept concept = ontologyRepository.getConceptByIRI(typeStr, workspaceId);

            boolean isMapGlyphIcon = false;
            byte[] glyphIcon = getMapGlyphIcon(concept, workspaceId);
            if (glyphIcon != null) {
                isMapGlyphIcon = true;
            } else {
                glyphIcon = getGlyphIcon(concept, workspaceId);
                if (glyphIcon == null) {
                    response.respondWithNotFound();
                    return;
                }
            }

            imageData = getMarkerImage(new ByteArrayInputStream(glyphIcon), scale, selected, heading, isMapGlyphIcon);
            imageCache.put(cacheKey, imageData);
        }

        response.setHeader("Cache-Control", "max-age=" + (5 * 60));
        response.write(imageData);
    }

    private int roundHeadingAngle(double heading) {
        while (heading < 0.0) {
            heading += 360.0;
        }
        while (heading > 360.0) {
            heading -= 360.0;
        }
        return (int) (Math.round(heading / 10.0) * 10.0);
    }

    private byte[] getMarkerImage(InputStream resource, long scale, boolean selected, int heading, boolean isMapGlyphIcon) throws IOException {
        BufferedImage resourceImage = ImageIO.read(resource);
        if (resourceImage == null) {
            return null;
        }

        if (heading != 0) {
            resourceImage = rotateImage(resourceImage, heading);
        }

        BufferedImage backgroundImage = getBackgroundImage(scale, selected);
        if (backgroundImage == null) {
            return null;
        }
        int[] resourceImageDim = new int[]{resourceImage.getWidth(), resourceImage.getHeight()};

        BufferedImage image = new BufferedImage(backgroundImage.getWidth(), backgroundImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        if (isMapGlyphIcon) {
            int[] boundary = new int[]{backgroundImage.getWidth(), backgroundImage.getHeight()};
            int[] scaledDims = thumbnailRepository.getScaledDimension(resourceImageDim, boundary);
            g.drawImage(resourceImage, 0, 0, scaledDims[0], scaledDims[1], null);
        } else {
            g.drawImage(backgroundImage, 0, 0, backgroundImage.getWidth(), backgroundImage.getHeight(), null);
            int size = image.getWidth() * 2 / 3;
            int[] boundary = new int[]{size, size};
            int[] scaledDims = thumbnailRepository.getScaledDimension(resourceImageDim, boundary);
            int x = (backgroundImage.getWidth() - scaledDims[0]) / 2;
            int y = (backgroundImage.getWidth() - scaledDims[1]) / 2;
            g.drawImage(resourceImage, x, y, scaledDims[0], scaledDims[1], null);
        }
        g.dispose();
        return imageToBytes(image);
    }

    private BufferedImage rotateImage(BufferedImage image, int angleDeg) {
        BufferedImage rotatedImage = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        Graphics2D g = rotatedImage.createGraphics();
        g.rotate(Math.toRadians(angleDeg), rotatedImage.getWidth() / 2, rotatedImage.getHeight() / 2);
        g.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
        g.dispose();
        return rotatedImage;
    }

    private BufferedImage getBackgroundImage(long scale, boolean selected) throws IOException {
        String imageFileName;
        if (scale == 1) {
            imageFileName = selected ? "marker-background-selected.png" : "marker-background.png";
        } else if (scale == 2) {
            imageFileName = selected ? "marker-background-selected-2x.png" : "marker-background-2x.png";
        } else {
            return null;
        }

        try (InputStream in = MapMarkerImage.class.getResourceAsStream(imageFileName)) {
            checkNotNull(in, "Could not find resource: " + imageFileName);
            return ImageIO.read(in);
        }
    }

    private byte[] imageToBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream imageData = new ByteArrayOutputStream();
        ImageIO.write(image, "png", imageData);
        imageData.close();
        return imageData.toByteArray();
    }

    private byte[] getMapGlyphIcon(Concept concept, String workspaceId) {
        byte[] mapGlyphIcon = null;
        for (Concept con = concept; mapGlyphIcon == null && con != null; con = ontologyRepository.getParentConcept(con, workspaceId)) {
            mapGlyphIcon = con.getMapGlyphIcon();
        }
        return mapGlyphIcon;
    }

    private byte[] getGlyphIcon(Concept concept, String workspaceId) {
        byte[] glyphIcon = null;
        for (Concept con = concept; glyphIcon == null && con != null; con = ontologyRepository.getParentConcept(con, workspaceId)) {
            glyphIcon = con.hasGlyphIconResource() ? con.getGlyphIcon() : null;
        }
        return glyphIcon;
    }
}
