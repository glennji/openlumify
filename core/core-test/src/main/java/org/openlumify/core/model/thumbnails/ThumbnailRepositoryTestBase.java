package org.openlumify.core.model.thumbnails;

import org.junit.Test;
import org.vertexium.Authorizations;
import org.vertexium.Vertex;
import org.vertexium.Visibility;
import org.vertexium.property.StreamingPropertyValue;
import org.openlumify.core.model.properties.OpenLumifyProperties;
import org.openlumify.core.util.OpenLumifyInMemoryTestBase;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public abstract class ThumbnailRepositoryTestBase extends OpenLumifyInMemoryTestBase {
    public abstract ThumbnailRepository getThumbnailRepository();

    @Test
    public void testCreateThumbnail() {
        Authorizations authorizations = getAuthorizationRepository().getGraphAuthorizations(getUserRepository().getSystemUser());
        Vertex artifactVertex = getGraph().addVertex("v1", new Visibility(""), authorizations);
        StreamingPropertyValue value = StreamingPropertyValue.create(getClass().getResourceAsStream("/org/openlumify/core/model/thumbnails/sample-image.jpg"), byte[].class);
        OpenLumifyProperties.RAW.setProperty(artifactVertex, value, new Visibility(""), authorizations);

        InputStream in = OpenLumifyProperties.RAW.getPropertyValue(artifactVertex).getInputStream();
        Thumbnail thumbnail = getThumbnailRepository().createThumbnail(
                artifactVertex,
                OpenLumifyProperties.RAW.getPropertyName(),
                "raw",
                in,
                new int[]{200, 200},
                getUserRepository().getSystemUser()
        );
        assertNotNull(thumbnail);

        int width = thumbnail.getImage().getWidth();
        int height = thumbnail.getImage().getHeight();
        thumbnail = getThumbnailRepository().getThumbnail(
                artifactVertex.getId(),
                "raw",
                width,
                height,
                "workspace1",
                getUserRepository().getSystemUser());
        assertEquals("jpg", thumbnail.getFormat());
        assertEquals(width, thumbnail.getImage().getWidth());
        assertEquals(height, thumbnail.getImage().getHeight());
    }
}
