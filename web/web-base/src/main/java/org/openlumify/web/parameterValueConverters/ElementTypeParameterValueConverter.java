package org.openlumify.web.parameterValueConverters;

import org.openlumify.webster.DefaultParameterValueConverter;
import org.vertexium.ElementType;

public class ElementTypeParameterValueConverter implements DefaultParameterValueConverter.Converter<ElementType> {
    @Override
    public ElementType convert(Class parameterType, String parameterName, String[] value) {
        return ElementType.valueOf(value[0].toUpperCase());
    }
}
