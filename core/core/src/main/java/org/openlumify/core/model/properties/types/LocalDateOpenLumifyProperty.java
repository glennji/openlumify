package org.openlumify.core.model.properties.types;

import java.time.LocalDate;
import java.util.Date;

/**
 * A multi-value property that converts a java.time.LocalDate object, which represents only a date without time
 * information, to an appropriate value for storage in Vertexium.
 */
public class LocalDateOpenLumifyProperty
        extends OpenLumifyProperty<LocalDate, Date>
        implements WrapsLocalDate {

    public LocalDateOpenLumifyProperty(String propertyName) {
        super(propertyName);
    }

    @Override
    public Date wrap(LocalDate localDate) {
        return WrapsLocalDate.super.wrap(localDate);
    }

    @Override
    public LocalDate unwrap(Object value) {
        return WrapsLocalDate.super.unwrap(value);
    }

    public static LocalDate now() {
        return WrapsLocalDate.now();
    }
}
