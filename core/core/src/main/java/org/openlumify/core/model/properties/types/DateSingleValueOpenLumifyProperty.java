package org.openlumify.core.model.properties.types;

import java.util.Date;

/**
 * A OpenLumifyProperty that converts a legacy java.util.Date object, which represents an instant in time, to an
 * appropriate value for storage in Vertexium.
 */
public class DateSingleValueOpenLumifyProperty extends IdentitySingleValueOpenLumifyProperty<Date> {
    public DateSingleValueOpenLumifyProperty(String propertyName) {
        super(propertyName);
    }
}
