package org.openlumify.core.exception;

public class OpenLumifyResourceNotFoundException extends OpenLumifyException {
    private final Object resourceId;

    public OpenLumifyResourceNotFoundException(String message) {
        super(message);
        this.resourceId = null;
    }

    public OpenLumifyResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.resourceId = null;
    }

    public OpenLumifyResourceNotFoundException(String message, Object resourceId) {
        super(message);
        this.resourceId = resourceId;
    }


    public Object getResourceId() {
        return resourceId;
    }
}
