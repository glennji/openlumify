package org.openlumify.web;

import org.visallo.webster.handlers.CSRFHandler;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class OpenLumifyCsrfHandler extends CSRFHandler {
    public static final String PARAMETER_NAME = "csrfToken";
    public static final String HEADER_NAME = "OpenLumify-CSRF-Token";

    public OpenLumifyCsrfHandler() {
        super(PARAMETER_NAME, HEADER_NAME);
    }
}
