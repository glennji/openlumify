package org.visallo.web;

import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.config.Configuration;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class VisalloDefaultErrorHandler extends HttpServlet {
    public static final String GENERIC_ERROR_MESSAGE = "Unable to process request. Please contact your system administrator for more details.";

    private boolean devMode;

    @Override
    public void init() {
        Configuration config = InjectHelper.getInstance(Configuration.class);
        devMode = config.getBoolean(Configuration.DEV_MODE, Configuration.DEV_MODE_DEFAULT);
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");

        if (devMode) {
            writeDefaultError(request, response);
        } else {
            response.getWriter().write(GENERIC_ERROR_MESSAGE);

        }
    }

    private void writeDefaultError(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Exception ex = (Exception) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        if (ex != null) {
            ex.printStackTrace(response.getWriter());
        } else {
            String errorResponse = "Error ";
            Integer status = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
            String message = (String) request.getAttribute(RequestDispatcher.ERROR_MESSAGE);

            errorResponse += status;
            if (message != null && message.length() > 0) {
                errorResponse += " : " + message;
            }

            response.getWriter().write(errorResponse);
        }

    }
}
