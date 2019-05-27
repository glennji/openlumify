package org.openlumify.core.model.properties.types;

/**
 * A OpenLumifyProperty whose raw and Vertexium types are the same.
 */
public class IdentityOpenLumifyProperty<T> extends OpenLumifyProperty<T, T> {
    /**
     * Create a new IdentityOpenLumifyProperty.
     * @param propertyName the property name
     */
    public IdentityOpenLumifyProperty(final String propertyName) {
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
