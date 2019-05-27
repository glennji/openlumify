package org.openlumify.core.model.properties.types;

import org.vertexium.ExtendedDataRow;

public class IntegerOpenLumifyExtendedData extends IdentityOpenLumifyExtendedData<Integer> {
    public IntegerOpenLumifyExtendedData(String tableName, String propertyName) {
        super(tableName, propertyName);
    }

    public int getValue(ExtendedDataRow row, int defaultValue) {
        Integer nullable = getValue(row);
        if (nullable == null) {
            return defaultValue;
        }
        return nullable;
    }
}
