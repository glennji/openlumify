package org.openlumify.core.util;

import org.openlumify.core.model.properties.OpenLumifyProperties;
import org.vertexium.Metadata;
import org.vertexium.Property;

import java.util.Comparator;

public class ConfidencePropertyComparator implements Comparator<Property> {
    @Override
    public int compare(Property p1, Property p2) {
        Metadata p1meta = p1.getMetadata();
        Metadata p2meta = p2.getMetadata();

        if (p1meta == null && p2meta == null) {
            return 0;
        }
        if (p1meta == null) {
            return 1;
        }
        if (p2meta == null) {
            return -1;
        }

        double p1confidence = OpenLumifyProperties.CONFIDENCE_METADATA.getMetadataValue(p1meta, 0.5);
        double p2confidence = OpenLumifyProperties.CONFIDENCE_METADATA.getMetadataValue(p2meta, 0.5);

        return -Double.compare(p1confidence, p2confidence);
    }
}
