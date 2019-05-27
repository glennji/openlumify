package org.openlumify.web.webEventListeners;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openlumify.core.config.Configuration;
import org.openlumify.core.exception.OpenLumifyAccessDeniedException;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;
import org.openlumify.web.BadRequestException;
import org.openlumify.web.ResponseTypes;
import org.openlumify.web.OpenLumifyResponse;
import org.openlumify.web.WebApp;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ErrorHandlerWebEventListener extends DefaultWebEventListener {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(ErrorHandlerWebEventListener.class);
    public static final int PRIORITY = -1000;

    @Override
    public void error(WebApp app, HttpServletRequest request, HttpServletResponse response, Throwable e) throws ServletException, IOException {
        if (e.getCause() instanceof OpenLumifyResourceNotFoundException) {
            handleNotFound(response, (OpenLumifyResourceNotFoundException) e.getCause());
            return;
        }
        if (e.getCause() instanceof BadRequestException) {
            handleBadRequest(response, (BadRequestException) e.getCause());
            return;
        }
        if (e.getCause() instanceof OpenLumifyAccessDeniedException) {
            handleAccessDenied(response, (OpenLumifyAccessDeniedException) e.getCause());
            return;
        }
        if (handleIllegalState(request, response, e)) {
            return;
        }
        if (isClientAbortException(e)) {
            return;
        }

        String message = String.format("Unhandled exception for %s %s", request.getMethod(), request.getRequestURI());
        if (app.isDevModeEnabled()) {
            throw new ServletException(message, e);
        } else {
            LOGGER.warn(message, e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private boolean isClientAbortException(Throwable e) {
        // Need to use a string here because ClientAbortException is a Tomcat specific exception
        if (e.getClass().getName().equals("org.apache.catalina.connector.ClientAbortException")) {
            return true;
        }
        if (e.getCause() != null) {
            return isClientAbortException(e.getCause());
        }
        return false;
    }

    private boolean handleIllegalState(HttpServletRequest request, HttpServletResponse response, Throwable e) throws IOException {
        boolean isMultipart = request.getContentType() != null && request.getContentType().startsWith("multipart/");
        if (isMultipart) {
            String TOMCAT_MAX_REQUEST_MESSAGE = "$SizeLimitExceededException";
            String TOMCAT_MAX_FILE_MESSAGE = "$FileSizeLimitExceededException";
            Throwable cause = e.getCause() == null ? e : e.getCause();
            String message = cause == null ? null : cause.getMessage();

            if (message != null && (
                    message.contains(TOMCAT_MAX_FILE_MESSAGE) || message.contains(TOMCAT_MAX_REQUEST_MESSAGE))) {
                long bytesToMB = 1024 * 1024;
                String errorMessage = String.format(
                        "Uploaded file(s) are too large. " +
                                "Limits are set to %dMB per file and %dMB total for all files",
                        Configuration.DEFAULT_MULTIPART_MAX_FILE_SIZE / bytesToMB,
                        Configuration.DEFAULT_MULTIPART_MAX_REQUEST_SIZE / bytesToMB
                );
                LOGGER.error(message, cause);
                handleBadRequest(response, new BadRequestException("files", errorMessage));
                return true;
            }
        }
        return false;
    }

    private void handleBadRequest(HttpServletResponse response, BadRequestException badRequestException) {
        LOGGER.error("bad request", badRequestException);
        JSONObject error = new JSONObject();
        error.put(badRequestException.getParameterName(), badRequestException.getMessage());
        if (badRequestException.getInvalidValues() != null) {
            JSONArray values = new JSONArray();
            for (String v : badRequestException.getInvalidValues()) {
                values.put(v);
            }
            error.put("invalidValues", values);
        }
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        OpenLumifyResponse.configureResponse(ResponseTypes.JSON_OBJECT, response, error);
    }

    private void handleAccessDenied(HttpServletResponse response, OpenLumifyAccessDeniedException accessDenied) throws IOException {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, accessDenied.getMessage());
    }

    private void handleNotFound(HttpServletResponse response, OpenLumifyResourceNotFoundException notFoundException) throws IOException {
        response.sendError(HttpServletResponse.SC_NOT_FOUND, notFoundException.getMessage());
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }
}
