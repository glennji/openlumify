package org.openlumify.graphCheck.rules;

import org.vertexium.Element;
import org.vertexium.Property;
import org.openlumify.core.model.properties.OpenLumifyProperties;
import org.openlumify.graphCheck.DefaultGraphCheckRule;
import org.openlumify.graphCheck.GraphCheckContext;

public class HasRequiredMetadataGraphCheckRule extends DefaultGraphCheckRule {
    @Override
    public void visitElement(GraphCheckContext ctx, Element element) {
        checkElementHasProperty(ctx, element, OpenLumifyProperties.MODIFIED_BY.getPropertyName());
        checkElementHasProperty(ctx, element, OpenLumifyProperties.MODIFIED_DATE.getPropertyName());
        checkElementHasProperty(ctx, element, OpenLumifyProperties.VISIBILITY_JSON.getPropertyName());
    }

    @Override
    public void visitProperty(GraphCheckContext ctx, Element element, Property property) {
        if (!property.getName().equals(OpenLumifyProperties.CONCEPT_TYPE.getPropertyName()) &&
                !property.getName().equals(OpenLumifyProperties.MODIFIED_BY.getPropertyName()) &&
                !property.getName().equals(OpenLumifyProperties.MODIFIED_DATE.getPropertyName()) &&
                !property.getName().equals(OpenLumifyProperties.VISIBILITY_JSON.getPropertyName())) {
            checkPropertyHasMetadata(ctx, element, property, OpenLumifyProperties.MODIFIED_BY_METADATA.getMetadataKey());
            checkPropertyHasMetadata(ctx, element, property, OpenLumifyProperties.MODIFIED_DATE_METADATA.getMetadataKey());
        }
    }
}
