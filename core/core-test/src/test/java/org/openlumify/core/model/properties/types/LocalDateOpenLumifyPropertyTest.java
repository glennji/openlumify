package org.openlumify.core.model.properties.types;

public class LocalDateOpenLumifyPropertyTest extends LocalDateOpenLumifyPropertyTestBase {
    @Override
    protected WrapsLocalDate createOpenLumifyProperty() {
        return new LocalDateOpenLumifyProperty("name");
    }
}
