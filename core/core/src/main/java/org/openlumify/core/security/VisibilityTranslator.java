package org.openlumify.core.security;

import org.vertexium.Visibility;
import org.openlumify.web.clientapi.model.VisibilityJson;

public abstract class VisibilityTranslator {
    public static final String JSON_SOURCE = "source";
    public static final String JSON_WORKSPACES = "workspaces";

    public abstract OpenLumifyVisibility toVisibility(VisibilityJson visibilityJson);

    public abstract OpenLumifyVisibility toVisibility(String visibilitySource);

    public abstract Visibility toVisibilityNoSuperUser(VisibilityJson visibilityJson);

    public abstract Visibility getDefaultVisibility();
}
