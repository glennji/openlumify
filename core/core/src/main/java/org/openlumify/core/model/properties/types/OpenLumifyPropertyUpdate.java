package org.openlumify.core.model.properties.types;

import org.vertexium.mutation.ElementMutation;

public class OpenLumifyPropertyUpdate {
    private final OpenLumifyPropertyBase property;
    private final String propertyKey;

    public OpenLumifyPropertyUpdate(OpenLumifyProperty property, String propertyKey) {
        this.property = property;
        this.propertyKey = propertyKey;
    }

    public OpenLumifyPropertyUpdate(SingleValueOpenLumifyProperty property) {
        this.property = property;
        this.propertyKey = ElementMutation.DEFAULT_KEY;
    }

    public OpenLumifyPropertyBase getProperty() {
        return property;
    }

    public String getPropertyKey() {
        return propertyKey;
    }

    public String getPropertyName() {
        return getProperty().getPropertyName();
    }

    @Override
    public String toString() {
        return "OpenLumifyPropertyUpdate{" +
                "property=" + property.getPropertyName() +
                ", propertyKey='" + propertyKey + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        OpenLumifyPropertyUpdate that = (OpenLumifyPropertyUpdate) o;

        if (!property.equals(that.property)) {
            return false;
        }
        if (!propertyKey.equals(that.propertyKey)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = property.hashCode();
        result = 31 * result + propertyKey.hashCode();
        return result;
    }
}
