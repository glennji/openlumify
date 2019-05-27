package org.openlumify.web.webEventListeners;

import org.openlumify.core.util.OpenLumifyPlugin;
import org.openlumify.web.WebApp;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@OpenLumifyPlugin(disabledByDefault = true)
public class SetThreadNameWebEventListener extends DefaultWebEventListener {
    public static final int PRIORITY = -1000;
    private static final String THREAD_NAME_PREFIX = "http-";

    @Override
    public void before(WebApp app, HttpServletRequest request, HttpServletResponse response) {
        Thread.currentThread().setName(getNewThreadName(request));
    }

    private String getNewThreadName(HttpServletRequest request) {
        return THREAD_NAME_PREFIX + request.getRequestURI();
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }
}
