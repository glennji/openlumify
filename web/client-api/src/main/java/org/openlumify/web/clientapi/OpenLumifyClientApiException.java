package org.openlumify.web.clientapi;

public class OpenLumifyClientApiException extends RuntimeException {
    public OpenLumifyClientApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public OpenLumifyClientApiException(String message) {
        super(message);
    }
}
