package org.openlumify.graphCheck.rules;

import org.vertexium.Element;
import org.vertexium.ExtendedDataRow;
import org.vertexium.Property;
import org.vertexium.Visibility;
import org.openlumify.core.security.OpenLumifyVisibility;
import org.openlumify.graphCheck.DefaultGraphCheckRule;
import org.openlumify.graphCheck.GraphCheckContext;

public class SystemVisibilityGraphCheckRule extends DefaultGraphCheckRule {
    @Override
    public void visitElement(GraphCheckContext ctx, Element element) {
        Visibility visibility = element.getVisibility();
        if (!visibleToSuperUser(visibility)) {
            ctx.reportError(
                    this,
                    element,
                    "Missing super user authorizations \"%s\" found \"%s\"",
                    OpenLumifyVisibility.SUPER_USER_VISIBILITY_STRING,
                    visibility.getVisibilityString()
            );
        }
    }

    @Override
    public void visitProperty(GraphCheckContext ctx, Element element, Property property) {
        Visibility visibility = property.getVisibility();
        if (!visibleToSuperUser(visibility)) {
            ctx.reportError(
                    this,
                    element,
                    property,
                    "Missing super user authorizations \"%s\" found \"%s\"",
                    OpenLumifyVisibility.SUPER_USER_VISIBILITY_STRING,
                    visibility.getVisibilityString()
            );
        }
    }

    @Override
    public void visitProperty(GraphCheckContext ctx, Element element, String tableName, ExtendedDataRow row, Property property) {
        Visibility visibility = property.getVisibility();
        if (!visibleToSuperUser(visibility)) {
            ctx.reportError(
                    this,
                    row,
                    property,
                    "Missing super user authorizations \"%s\" found \"%s\"",
                    OpenLumifyVisibility.SUPER_USER_VISIBILITY_STRING,
                    visibility.getVisibilityString()
            );
        }
    }

    private boolean visibleToSuperUser(Visibility visibility) {
        if (visibility.getVisibilityString().length() == 0) {
            return true;
        }
        return visibility.hasAuthorization(OpenLumifyVisibility.SUPER_USER_VISIBILITY_STRING);
    }
}
