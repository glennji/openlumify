package org.openlumify.core.model.properties.types;

public class OpenLumifyPropertyUpdateUnhide extends OpenLumifyPropertyUpdate {
    public OpenLumifyPropertyUpdateUnhide(OpenLumifyProperty property, String propertyKey) {
        super(property, propertyKey);
    }

    public OpenLumifyPropertyUpdateUnhide(SingleValueOpenLumifyProperty property) {
        super(property);
    }
}
