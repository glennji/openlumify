package org.openlumify.core.model.properties.types;

import org.vertexium.ExtendedDataRow;

public class BooleanOpenLumifyExtendedData extends IdentityOpenLumifyExtendedData<Boolean> {
    public BooleanOpenLumifyExtendedData(String tableName, String propertyName) {
        super(tableName, propertyName);
    }

    public boolean getValue(ExtendedDataRow row, boolean defaultValue) {
        Boolean nullable = getValue(row);
        if (nullable == null) {
            return defaultValue;
        }
        return nullable;
    }
}
