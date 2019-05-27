package org.openlumify.web.clientapi;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class UserNameAndPasswordOpenLumifyApi extends FormLoginOpenLumifyApi {
    private final String username;
    private final String password;

    public UserNameAndPasswordOpenLumifyApi(String basePath, String username, String password) {
        this(basePath, username, password, false);
    }

    public UserNameAndPasswordOpenLumifyApi(String basePath, String username, String password, boolean ignoreSslErrors) {
        super(basePath, ignoreSslErrors);
        this.username = username;
        this.password = password;
        logIn();
    }

    @Override
    protected String getLoginFormBody() {
        try {
            return "username=" + URLEncoder.encode(username, "UTF-8") + "&" + "password=" + URLEncoder.encode(password, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            throw new OpenLumifyClientApiException("Failed to encode username and/or password", uee);
        }
    }
}
