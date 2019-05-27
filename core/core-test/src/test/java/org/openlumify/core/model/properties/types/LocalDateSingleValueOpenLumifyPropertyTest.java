package org.openlumify.core.model.properties.types;

public class LocalDateSingleValueOpenLumifyPropertyTest extends LocalDateOpenLumifyPropertyTestBase {
    @Override
    protected WrapsLocalDate createOpenLumifyProperty() {
        return new LocalDateSingleValueOpenLumifyProperty("name");
    }
}
