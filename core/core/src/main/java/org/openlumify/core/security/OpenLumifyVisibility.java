package org.openlumify.core.security;

import org.vertexium.Visibility;

public class OpenLumifyVisibility {
    public static final String SUPER_USER_VISIBILITY_STRING = "openlumify";
    private final Visibility visibility;

    public OpenLumifyVisibility() {
        this.visibility = new Visibility("");
    }

    public OpenLumifyVisibility(String visibility) {
        if (visibility == null || visibility.length() == 0) {
            this.visibility = new Visibility("");
        } else {
            this.visibility = addSuperUser(visibility);
        }
    }

    public OpenLumifyVisibility(Visibility visibility) {
        if (visibility == null || visibility.getVisibilityString().length() == 0
                || visibility.getVisibilityString().contains(SUPER_USER_VISIBILITY_STRING)) {
            this.visibility = visibility;
        } else {
            this.visibility = addSuperUser(visibility.getVisibilityString());
        }
    }

    public Visibility getVisibility() {
        return visibility;
    }

    private Visibility addSuperUser(String visibility) {
        return new Visibility("(" + visibility + ")|" + SUPER_USER_VISIBILITY_STRING);
    }

    @Override
    public String toString() {
        return getVisibility().toString();
    }

    public static Visibility and(Visibility visibility, String additionalVisibility) {
        if (visibility.getVisibilityString().length() == 0) {
            return new Visibility(additionalVisibility);
        }
        return new Visibility("(" + visibility.getVisibilityString() + ")&(" + additionalVisibility + ")");
    }

    public static Visibility or(Visibility visibility, String additionalVisibility) {
        if (visibility.getVisibilityString().length() == 0) {
            return new Visibility(additionalVisibility);
        }
        return new Visibility("(" + visibility.getVisibilityString() + ")|(" + additionalVisibility + ")");
    }
}
