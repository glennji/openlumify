package org.openlumify.web.parameterProviders;

import org.visallo.webster.HandlerChain;
import org.visallo.webster.parameterProviders.ParameterProvider;
import org.visallo.webster.parameterProviders.ParameterProviderFactory;
import org.openlumify.web.OpenLumifyResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class OpenLumifyResponseParameterProviderFactory extends ParameterProviderFactory<OpenLumifyResponse> {
    private static final ParameterProvider<OpenLumifyResponse> PARAMETER_PROVIDER = new ParameterProvider<OpenLumifyResponse>() {
        @Override
        public OpenLumifyResponse getParameter(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) {
            return new OpenLumifyResponse(request, response);
        }
    };

    @Override
    public boolean isHandled(Method handleMethod, Class<? extends OpenLumifyResponse> parameterType, Annotation[] parameterAnnotations) {
        return OpenLumifyResponse.class.isAssignableFrom(parameterType);
    }

    @Override
    public ParameterProvider<OpenLumifyResponse> createParameterProvider(Method handleMethod, Class<?> parameterType, Annotation[] parameterAnnotations) {
        return PARAMETER_PROVIDER;
    }
}
