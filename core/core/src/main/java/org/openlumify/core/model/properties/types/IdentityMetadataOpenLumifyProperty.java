package org.openlumify.core.model.properties.types;

public class IdentityMetadataOpenLumifyProperty<T> extends MetadataOpenLumifyProperty<T, T> {
    /**
     * Create a new IdentityOpenLumifyProperty.
     *
     * @param propertyName the property name
     */
    public IdentityMetadataOpenLumifyProperty(final String propertyName) {
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
