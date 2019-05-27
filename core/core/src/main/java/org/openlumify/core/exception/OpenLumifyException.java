package org.openlumify.core.exception;

public class OpenLumifyException extends RuntimeException {
    private static final long serialVersionUID = -4322348262201847859L;

    public OpenLumifyException() {
    }

    public OpenLumifyException(String message) {
        super(message);
    }

    public OpenLumifyException(String message, Throwable cause) {
        super(message, cause);
    }
}
