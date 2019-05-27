package org.openlumify.web.webEventListeners;

import org.openlumify.web.WebApp;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface WebEventListener {
    /**
     * Event fired before the web request is handled
     */
    void before(WebApp app, HttpServletRequest request, HttpServletResponse response);

    /**
     * Event fired after the web request is successfully handled
     */
    void after(WebApp app, HttpServletRequest request, HttpServletResponse response);

    /**
     * Event fired if an error occurred while processing the web request
     */
    void error(WebApp app, HttpServletRequest request, HttpServletResponse response, Throwable error) throws ServletException, IOException;

    /**
     * Event is fired after the web request is handled regardless of success
     */
    void always(WebApp app, HttpServletRequest request, HttpServletResponse response);

    /**
     * Priority of the listener. The lower the priority the sooner it will fire before the request and the later it
     * will fire after the request. Negative numbers are reserved by code OpenLumify and should not be used.
     */
    int getPriority();
}
