package org.openlumify.core.model.properties.types;

import org.openlumify.web.clientapi.model.VisibilityJson;

public class VisibilityJsonOpenLumifyProperty extends ClientApiSingleValueOpenLumifyProperty<VisibilityJson> {
    public VisibilityJsonOpenLumifyProperty(String key) {
        super(key, VisibilityJson.class);
    }
}
