package org.openlumify.core.model.graph;

import org.openlumify.core.security.OpenLumifyVisibility;
import org.vertexium.Element;
import org.vertexium.mutation.ExistingElementMutation;

public class VisibilityAndElementMutation<T extends Element> {
    public final ExistingElementMutation<T> elementMutation;
    public final OpenLumifyVisibility visibility;

    public VisibilityAndElementMutation(OpenLumifyVisibility visibility, ExistingElementMutation<T> elementMutation) {
        this.visibility = visibility;
        this.elementMutation = elementMutation;
    }
}
