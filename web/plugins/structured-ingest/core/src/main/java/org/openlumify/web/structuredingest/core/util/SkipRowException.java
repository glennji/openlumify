package org.openlumify.web.structuredingest.core.util;

import org.openlumify.core.exception.OpenLumifyException;

public class SkipRowException extends OpenLumifyException {

    public SkipRowException(String message) {
        super(message);
    }

    public SkipRowException(String message, Throwable cause) {
        super(message, cause);
    }
}
