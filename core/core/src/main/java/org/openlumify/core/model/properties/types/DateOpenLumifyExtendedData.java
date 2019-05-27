package org.openlumify.core.model.properties.types;

import org.vertexium.ExtendedDataRow;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

public class DateOpenLumifyExtendedData extends IdentityOpenLumifyExtendedData<Date> {
    public DateOpenLumifyExtendedData(String tableName, String propertyName) {
        super(tableName, propertyName);
    }

    public ZonedDateTime getValueDateTimeUtc(ExtendedDataRow row) {
        Date value = getValue(row);
        if (value == null) {
            return null;
        }
        return ZonedDateTime.ofInstant(value.toInstant(), ZoneOffset.UTC);
    }
}
