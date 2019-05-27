package org.openlumify.web.routes.security;

import com.google.common.base.Charsets;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;
import org.openlumify.webster.ParameterizedHandler;
import org.openlumify.webster.annotations.Handle;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.IOException;

public class ContentSecurityPolicyReport implements ParameterizedHandler {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(ContentSecurityPolicyReport.class);

    @Handle
    public void reportViolation(HttpServletRequest request) {
        // https://developer.mozilla.org/en-US/docs/Web/HTTP/CSP
        try {
            String json = IOUtils.toString(request.getInputStream(), Charsets.UTF_8);
            JSONObject input = new JSONObject(json);
            JSONObject report = input.getJSONObject("csp-report");
            LOGGER.error(
                    "Content-Security-Policy violation: '%s' Violated rule: '%s'",
                    report.getString("blocked-uri"),
                    report.getString("violated-directive")
            );
        } catch (JSONException jse) {
            throw new OpenLumifyException("Unable to process Content-Security-Policy report", jse);
        } catch (IOException e) {
            throw new OpenLumifyException("Unable to process Content-Security-Policy report", e);
        }

    }
}
