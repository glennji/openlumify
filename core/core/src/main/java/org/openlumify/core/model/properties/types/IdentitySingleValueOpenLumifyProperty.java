package org.openlumify.core.model.properties.types;

public class IdentitySingleValueOpenLumifyProperty<T> extends SingleValueOpenLumifyProperty<T, T> {
    public IdentitySingleValueOpenLumifyProperty(final String propertyName) {
        super(propertyName);
    }

    @Override
    public T wrap(final T value) {
        return value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T unwrap(final Object value) {
        return (T) value;
    }
}
