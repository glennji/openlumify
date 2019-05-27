package org.openlumify.core.model.properties.types;

import org.openlumify.web.clientapi.model.DirectoryEntity;
import org.openlumify.web.clientapi.util.ClientApiConverter;

public class DirectoryEntityOpenLumifyProperty extends OpenLumifyProperty<DirectoryEntity, String> {
    public DirectoryEntityOpenLumifyProperty(String key) {
        super(key);
    }

    @Override
    public String wrap(DirectoryEntity value) {
        return ClientApiConverter.clientApiToString(value);
    }

    @Override
    public DirectoryEntity unwrap(Object value) {
        if (value == null) {
            return null;
        }
        String valueStr;
        if (value instanceof String) {
            valueStr = (String) value;
        } else {
            valueStr = value.toString();
        }
        return ClientApiConverter.toClientApi(valueStr, DirectoryEntity.class);
    }
}
