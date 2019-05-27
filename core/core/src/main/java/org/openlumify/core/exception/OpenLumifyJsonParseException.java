package org.openlumify.core.exception;

import org.json.JSONException;

public class OpenLumifyJsonParseException extends RuntimeException {
    public OpenLumifyJsonParseException(String jsonString, JSONException cause) {
        super("Could not parse json string: " + jsonString, cause);
    }
}
