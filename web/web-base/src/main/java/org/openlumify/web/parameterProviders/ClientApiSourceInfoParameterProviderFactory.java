package org.openlumify.web.parameterProviders;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.core.config.Configuration;
import org.openlumify.web.clientapi.model.ClientApiSourceInfo;
import org.openlumify.webster.HandlerChain;
import org.openlumify.webster.parameterProviders.ParameterProvider;
import org.openlumify.webster.parameterProviders.ParameterProviderFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

@Singleton
public class ClientApiSourceInfoParameterProviderFactory extends ParameterProviderFactory<ClientApiSourceInfo> {
    private ParameterProvider<ClientApiSourceInfo> parameterProvider;

    @Inject
    public ClientApiSourceInfoParameterProviderFactory(Configuration configuration) {
        parameterProvider = new OpenLumifyBaseParameterProvider<ClientApiSourceInfo>(configuration) {
            @Override
            public ClientApiSourceInfo getParameter(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) {
                final String sourceInfoString = getOptionalParameter(request, "sourceInfo");
                return ClientApiSourceInfo.fromString(sourceInfoString);
            }
        };
    }

    @Override
    public boolean isHandled(Method handleMethod, Class<? extends ClientApiSourceInfo> parameterType, Annotation[] parameterAnnotations) {
        return ClientApiSourceInfo.class.isAssignableFrom(parameterType);
    }

    @Override
    public ParameterProvider<ClientApiSourceInfo> createParameterProvider(Method handleMethod, Class<?> parameterType, Annotation[] parameterAnnotations) {
        return parameterProvider;
    }
}
