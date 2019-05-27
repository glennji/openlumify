package org.openlumify.web.util;

import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Visibility;
import org.openlumify.core.security.VisibilityTranslator;
import org.openlumify.core.user.User;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;
import org.openlumify.web.BadRequestException;
import org.openlumify.web.clientapi.model.VisibilityJson;

import java.util.ResourceBundle;

public class VisibilityValidator {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(VisibilityValidator.class);

    public static Visibility validate(
            Graph graph,
            VisibilityTranslator visibilityTranslator,
            ResourceBundle resourceBundle,
            String visibilitySource,
            User user,
            Authorizations authorizations
    ) throws BadRequestException {
        Visibility visibility = visibilityTranslator.toVisibility(visibilitySource).getVisibility();
        return validate(graph, resourceBundle, visibility, user, authorizations);
    }

    public static Visibility validate(
            Graph graph,
            VisibilityTranslator visibilityTranslator,
            ResourceBundle resourceBundle,
            VisibilityJson visibilityJson,
            User user,
            Authorizations authorizations
    ) throws BadRequestException {
        Visibility visibility = visibilityTranslator.toVisibility(visibilityJson).getVisibility();
        return validate(graph, resourceBundle, visibility, user, authorizations);
    }

    public static Visibility validate(
            Graph graph,
            ResourceBundle resourceBundle,
            Visibility visibility,
            User user,
            Authorizations authorizations
    ) {
        if (!graph.isVisibilityValid(visibility, authorizations)) {
            LOGGER.warn("%s is not a valid visibility for %s user", visibility, user.getDisplayName());
            throw new BadRequestException("visibilitySource", resourceBundle.getString("visibility.invalid"));
        }
        return visibility;
    }
}
