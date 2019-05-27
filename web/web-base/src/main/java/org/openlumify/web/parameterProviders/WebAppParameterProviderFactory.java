package org.openlumify.web.parameterProviders;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.core.config.Configuration;
import org.openlumify.web.WebApp;
import org.openlumify.webster.HandlerChain;
import org.openlumify.webster.parameterProviders.ParameterProvider;
import org.openlumify.webster.parameterProviders.ParameterProviderFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ResourceBundle;

@Singleton
public class WebAppParameterProviderFactory extends ParameterProviderFactory<WebApp> {
    private ParameterProvider<WebApp> parameterProvider;

    @Inject
    public WebAppParameterProviderFactory(Configuration configuration) {
        parameterProvider = new OpenLumifyBaseParameterProvider<WebApp>(configuration) {
            @Override
            public WebApp getParameter(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) {
                return getWebApp(request);
            }
        };
    }

    @Override
    public boolean isHandled(Method handleMethod, Class<? extends WebApp> parameterType, Annotation[] parameterAnnotations) {
        return ResourceBundle.class.isAssignableFrom(parameterType);
    }

    @Override
    public ParameterProvider<WebApp> createParameterProvider(Method handleMethod, Class<?> parameterType, Annotation[] parameterAnnotations) {
        return parameterProvider;
    }
}
