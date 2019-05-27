package org.openlumify.core.model.properties.types;

public class StringOpenLumifyExtendedData extends OpenLumifyExtendedData<String, String> {
    public StringOpenLumifyExtendedData(String tableName, String columnName) {
        super(tableName, columnName);
    }

    @Override
    public String rawToGraph(String value) {
        return value;
    }

    @Override
    public String graphToRaw(Object value) {
        return value.toString();
    }
}
