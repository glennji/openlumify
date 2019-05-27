package org.openlumify.core.ingest.graphProperty;

import org.openlumify.core.model.properties.types.OpenLumifyPropertyUpdate;
import org.openlumify.core.model.properties.types.OpenLumifyPropertyUpdateRemove;
import org.openlumify.core.model.properties.types.OpenLumifyPropertyUpdateUnhide;

public enum ElementOrPropertyStatus {
    HIDDEN,
    UNHIDDEN,
    DELETION,
    UPDATE;

    public static ElementOrPropertyStatus safeParse(String status) {
        try {
            if (status == null || status.length() == 0) {
                return ElementOrPropertyStatus.UPDATE;
            }
            return ElementOrPropertyStatus.valueOf(status);
        } catch (Exception ex) {
            return ElementOrPropertyStatus.UPDATE;
        }
    }

    public static ElementOrPropertyStatus getStatus (OpenLumifyPropertyUpdate propertyUpdate) {
        if (propertyUpdate instanceof OpenLumifyPropertyUpdateRemove && ((OpenLumifyPropertyUpdateRemove) propertyUpdate).isDeleted()) {
            return ElementOrPropertyStatus.DELETION;
        }
        if (propertyUpdate instanceof OpenLumifyPropertyUpdateRemove && ((OpenLumifyPropertyUpdateRemove) propertyUpdate).isHidden()) {
            return ElementOrPropertyStatus.HIDDEN;
        }

        if (propertyUpdate instanceof OpenLumifyPropertyUpdateUnhide) {
            return ElementOrPropertyStatus.UNHIDDEN;
        }

        return ElementOrPropertyStatus.UPDATE;
    }
}
