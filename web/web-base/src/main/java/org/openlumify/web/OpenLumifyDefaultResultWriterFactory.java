package org.openlumify.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.openlumify.core.config.Configuration;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.model.workspace.WorkspaceRepository;
import org.openlumify.core.security.ACLProvider;
import org.openlumify.core.trace.Trace;
import org.openlumify.core.trace.TraceSpan;
import org.openlumify.core.user.User;
import org.openlumify.web.clientapi.model.ClientApiObject;
import org.openlumify.web.clientapi.model.ClientApiWorkspace;
import org.openlumify.web.clientapi.util.ObjectMapperFactory;
import org.openlumify.web.parameterProviders.OpenLumifyBaseParameterProvider;
import org.visallo.webster.resultWriters.ResultWriter;
import org.visallo.webster.resultWriters.ResultWriterBase;
import org.visallo.webster.resultWriters.ResultWriterFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

@Singleton
public class OpenLumifyDefaultResultWriterFactory implements ResultWriterFactory {

    private final String responseHeaderXFrameOptions;
    private ACLProvider aclProvider;
    private WorkspaceRepository workspaceRepository;

    @Inject
    public OpenLumifyDefaultResultWriterFactory(
            ACLProvider aclProvider,
            WorkspaceRepository workspaceRepository,
            Configuration configuration
    ) {
        this.aclProvider = aclProvider;
        this.workspaceRepository = workspaceRepository;
        this.responseHeaderXFrameOptions = configuration.get(Configuration.WEB_RESPONSE_HEADER_X_FRAME_OPTIONS, null);
    }

    @Override
    public ResultWriter createResultWriter(Method handleMethod) {
        return new ResultWriterBase(handleMethod) {
            private boolean resultIsClientApiObject;
            private boolean resultIsInputStream;

            @Override
            protected String getContentType(Method handleMethod) {
                if (JSONObject.class.equals(handleMethod.getReturnType())) {
                    return "application/json";
                }
                if (ClientApiObject.class.isAssignableFrom(handleMethod.getReturnType())) {
                    resultIsClientApiObject = true;
                    return "application/json";
                }
                if (InputStream.class.isAssignableFrom(handleMethod.getReturnType())) {
                    resultIsInputStream = true;
                }
                return super.getContentType(handleMethod);
            }

            @Override
            protected void writeResult(HttpServletRequest request, HttpServletResponse response, Object result)
                    throws IOException {
                if (result != null) {
                    if (!response.containsHeader("X-Frame-Options")) {
                        response.addHeader("X-Frame-Options", responseHeaderXFrameOptions);
                    }
                    if (!response.containsHeader("X-Content-Type-Options")) {
                        response.addHeader("X-Content-Type-Options", "nosniff");
                    }
                    response.setCharacterEncoding("UTF-8");
                    if (resultIsClientApiObject || result instanceof JSONObject) {
                        response.addHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                        response.addHeader("Pragma", "no-cache");
                        response.addHeader("Expires", "0");
                    }
                    if (resultIsClientApiObject) {
                        ClientApiObject clientApiObject = (ClientApiObject) result;
                        try (TraceSpan ignored = Trace.start("aclProvider.appendACL")) {
                            if (clientApiObject != OpenLumifyResponse.SUCCESS) {
                                User user = CurrentUser.get(request);
                                String workspaceId;
                                if (clientApiObject instanceof ClientApiWorkspace) {
                                    workspaceId = ((ClientApiWorkspace)clientApiObject).getWorkspaceId();
                                } else {
                                    workspaceId = OpenLumifyBaseParameterProvider.getActiveWorkspaceIdOrDefault(request, workspaceRepository);
                                }
                                if (StringUtils.isEmpty(workspaceId)) {
                                    workspaceId = user == null ? null : user.getCurrentWorkspaceId();
                                }
                                clientApiObject = aclProvider.appendACL(clientApiObject, user, workspaceId);
                            }
                        }
                        String jsonObject;
                        try {
                            jsonObject = ObjectMapperFactory.getInstance().writeValueAsString(clientApiObject);
                        } catch (JsonProcessingException e) {
                            throw new OpenLumifyException("Could not convert clientApiObject to string", e);
                        }
                        response.getWriter().write(jsonObject);
                    } else if (resultIsInputStream) {
                        try (InputStream in = (InputStream) result) {
                            IOUtils.copy(in, response.getOutputStream());
                        } finally {
                            response.flushBuffer();
                        }
                    } else {
                        super.writeResult(request, response, result);
                    }
                }
            }
        };
    }
}
