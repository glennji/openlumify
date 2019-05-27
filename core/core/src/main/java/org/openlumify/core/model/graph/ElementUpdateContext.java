package org.openlumify.core.model.graph;

import org.vertexium.Edge;
import org.vertexium.Element;
import org.vertexium.Visibility;
import org.vertexium.mutation.EdgeMutation;
import org.vertexium.mutation.ElementMutation;
import org.vertexium.mutation.ExistingElementMutation;
import org.openlumify.core.model.properties.OpenLumifyProperties;
import org.openlumify.core.model.properties.types.PropertyMetadata;
import org.openlumify.core.model.properties.types.OpenLumifyPropertyUpdate;
import org.openlumify.core.security.VisibilityTranslator;
import org.openlumify.core.user.User;
import org.openlumify.web.clientapi.model.VisibilityJson;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.vertexium.util.Preconditions.checkNotNull;

public class ElementUpdateContext<T extends Element> {
    private final VisibilityTranslator visibilityTranslator;
    private final ElementMutation<T> mutation;
    private final User user;
    private final List<OpenLumifyPropertyUpdate> properties = new ArrayList<>();
    private final T element;

    public ElementUpdateContext(VisibilityTranslator visibilityTranslator, ElementMutation<T> mutation, User user) {
        this.visibilityTranslator = visibilityTranslator;
        this.mutation = mutation;
        this.user = user;
        if (mutation instanceof ExistingElementMutation) {
            element = ((ExistingElementMutation<T>) mutation).getElement();
        } else {
            element = null;
        }
    }

    public boolean isNewElement() {
        return getElement() == null;
    }

    public ElementMutation<T> getMutation() {
        return mutation;
    }

    public List<OpenLumifyPropertyUpdate> getProperties() {
        return properties;
    }

    public T getElement() {
        return element;
    }

    public void updateBuiltInProperties(
            Date modifiedDate,
            VisibilityJson visibilityJson
    ) {
        checkNotNull(user, "User cannot be null");
        Visibility defaultVisibility = visibilityTranslator.getDefaultVisibility();
        OpenLumifyProperties.MODIFIED_BY.updateProperty(this, user.getUserId(), defaultVisibility);
        OpenLumifyProperties.MODIFIED_DATE.updateProperty(this, modifiedDate, defaultVisibility);
        OpenLumifyProperties.VISIBILITY_JSON.updateProperty(this, visibilityJson, defaultVisibility);
    }

    public void updateBuiltInProperties(PropertyMetadata propertyMetadata) {
        updateBuiltInProperties(propertyMetadata.getModifiedDate(), propertyMetadata.getVisibilityJson());
    }

    public void setConceptType(String conceptType) {
        if (isEdgeMutation()) {
            throw new IllegalArgumentException("Cannot set concept type on edges");
        }

        Visibility defaultVisibility = visibilityTranslator.getDefaultVisibility();
        OpenLumifyProperties.CONCEPT_TYPE.updateProperty(this, conceptType, defaultVisibility);
    }

    private boolean isEdgeMutation() {
        if (getMutation() instanceof EdgeMutation) {
            return true;
        }
        if (getMutation() instanceof ExistingElementMutation) {
            ExistingElementMutation m = (ExistingElementMutation) getMutation();
            if (m.getElement() instanceof Edge) {
                return true;
            }
        }
        return false;
    }
}
