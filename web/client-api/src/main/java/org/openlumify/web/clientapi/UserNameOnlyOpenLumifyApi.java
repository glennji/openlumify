package org.openlumify.web.clientapi;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class UserNameOnlyOpenLumifyApi extends FormLoginOpenLumifyApi {
    protected final String username;

    public UserNameOnlyOpenLumifyApi(String basePath, String username) {
        this(basePath, username, false);
    }

    public UserNameOnlyOpenLumifyApi(String basePath, String username, boolean ignoreSslErrors) {
        super(basePath, ignoreSslErrors);
        this.username = username;
        logIn();
    }

    @Override
    protected String getLoginFormBody() {
        try {
            return "username=" + URLEncoder.encode(username, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            throw new OpenLumifyClientApiException("Failed to encode username", uee);
        }
    }
}
