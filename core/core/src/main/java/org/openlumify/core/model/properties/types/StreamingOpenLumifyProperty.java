package org.openlumify.core.model.properties.types;

import org.apache.commons.io.IOUtils;
import org.vertexium.Element;
import org.vertexium.property.StreamingPropertyValue;
import org.openlumify.core.exception.OpenLumifyException;

import java.io.IOException;

/**
 * An IdentityOpenLumifyProperty for StreamingPropertyValues.
 */
public class StreamingOpenLumifyProperty extends IdentityOpenLumifyProperty<StreamingPropertyValue> {
    /**
     * Create a new StreamingOpenLumifyProperty.
     *
     * @param key the property key
     */
    public StreamingOpenLumifyProperty(String key) {
        super(key);
    }

    public byte[] getFirstPropertyValueAsBytes(Element element) {
        StreamingPropertyValue propertyValue = getFirstPropertyValue(element);
        if (propertyValue == null) {
            return null;
        }
        try {
            return IOUtils.toByteArray(propertyValue.getInputStream());
        } catch (IOException e) {
            throw new OpenLumifyException("Could not get byte[] from StreamingPropertyValue", e);
        }
    }

    public String getFirstPropertyValueAsString(Element element) {
        StreamingPropertyValue propertyValue = getFirstPropertyValue(element);
        if (propertyValue == null) {
            return null;
        }
        try {
            return IOUtils.toString(propertyValue.getInputStream());
        } catch (IOException e) {
            throw new OpenLumifyException("Could not get string from StreamingPropertyValue", e);
        }
    }
}
