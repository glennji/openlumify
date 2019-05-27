package org.openlumify.core.model.properties.types;

import org.vertexium.ExtendedDataRow;

public class DoubleOpenLumifyExtendedData extends IdentityOpenLumifyExtendedData<Double> {
    public DoubleOpenLumifyExtendedData(String tableName, String propertyName) {
        super(tableName, propertyName);
    }

    public double getValue(ExtendedDataRow row, double defaultValue) {
        Double nullable = getValue(row);
        if (nullable == null) {
            return defaultValue;
        }
        return nullable;
    }
}
