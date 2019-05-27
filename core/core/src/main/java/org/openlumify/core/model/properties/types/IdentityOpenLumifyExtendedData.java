package org.openlumify.core.model.properties.types;

public abstract class IdentityOpenLumifyExtendedData<T> extends OpenLumifyExtendedData<T, T> {
    public IdentityOpenLumifyExtendedData(String tableName, String propertyName) {
        super(tableName, propertyName);
    }

    @Override
    public T rawToGraph(T value) {
        return value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T graphToRaw(final Object value) {
        return (T) value;
    }
}
