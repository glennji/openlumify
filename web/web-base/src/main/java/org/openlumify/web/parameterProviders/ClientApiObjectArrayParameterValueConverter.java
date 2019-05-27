package org.openlumify.web.parameterProviders;

import org.openlumify.webster.DefaultParameterValueConverter;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.web.clientapi.model.ClientApiObject;
import org.openlumify.web.clientapi.util.ObjectMapperFactory;

import java.io.IOException;

public class ClientApiObjectArrayParameterValueConverter extends DefaultParameterValueConverter.SingleValueConverter<ClientApiObject[]> {
    @Override
    public ClientApiObject[] convert(Class parameterType, String parameterName, String value) {
        try {
            return (ClientApiObject[]) ObjectMapperFactory.getInstance().readValue(value, parameterType);
        } catch (IOException ex) {
            throw new OpenLumifyException("Could not convert \"" + value + "\" to object of type " + parameterType.getName(), ex);
        }
    }
}
