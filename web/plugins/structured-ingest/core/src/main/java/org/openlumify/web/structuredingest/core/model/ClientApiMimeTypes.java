package org.openlumify.web.structuredingest.core.model;

import com.google.common.collect.Sets;
import org.openlumify.web.clientapi.model.ClientApiObject;

import java.util.Collection;
import java.util.Set;

public class ClientApiMimeTypes implements ClientApiObject {

    public Set<String> mimeTypes = Sets.newHashSet();

    public void addMimeTypes(Collection<String> newTypes) {
        mimeTypes.addAll(newTypes);
    }
}
