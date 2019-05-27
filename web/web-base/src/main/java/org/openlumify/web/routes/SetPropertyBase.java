package org.openlumify.web.routes;

import org.vertexium.Graph;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.model.properties.OpenLumifyProperties;
import org.openlumify.core.security.VisibilityTranslator;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public abstract class SetPropertyBase {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(SetPropertyBase.class);

    protected final Graph graph;
    protected final VisibilityTranslator visibilityTranslator;

    protected SetPropertyBase(Graph graph, VisibilityTranslator visibilityTranslator) {
        this.graph = graph;
        this.visibilityTranslator = visibilityTranslator;
    }

    protected boolean isCommentProperty(String propertyName) {
        return OpenLumifyProperties.COMMENT.isSameName(propertyName);
    }

    protected String createPropertyKey(String propertyName, Graph graph) {
        return isCommentProperty(propertyName) ? createCommentPropertyKey() : graph.getIdGenerator().nextId();
    }

    protected void checkRoutePath(String entityType, String propertyName, HttpServletRequest request) {
        boolean isComment = isCommentProperty(propertyName);
        if (isComment && request.getPathInfo().equals(String.format("/%s/property", entityType))) {
            throw new OpenLumifyException(String.format("Use /%s/comment to save comment properties", entityType));
        } else if (!isComment && request.getPathInfo().equals(String.format("/%s/comment", entityType))) {
            throw new OpenLumifyException(String.format("Use /%s/property to save non-comment properties", entityType));
        }
    }

    private static String createCommentPropertyKey() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(new Date());
    }
}
