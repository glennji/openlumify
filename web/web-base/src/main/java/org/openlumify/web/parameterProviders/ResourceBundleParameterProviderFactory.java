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
import java.util.Locale;
import java.util.ResourceBundle;

@Singleton
public class ResourceBundleParameterProviderFactory extends ParameterProviderFactory<ResourceBundle> {
    private ParameterProvider<ResourceBundle> parameterProvider;

    @Inject
    public ResourceBundleParameterProviderFactory(Configuration configuration) {
        parameterProvider = new OpenLumifyBaseParameterProvider<ResourceBundle>(configuration) {
            @Override
            public ResourceBundle getParameter(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) {
                WebApp webApp = getWebApp(request);
                Locale locale = getLocale(request);
                return webApp.getBundle(locale);
            }
        };
    }

    @Override
    public boolean isHandled(Method handleMethod, Class<? extends ResourceBundle> parameterType, Annotation[] parameterAnnotations) {
        return ResourceBundle.class.isAssignableFrom(parameterType);
    }

    @Override
    public ParameterProvider<ResourceBundle> createParameterProvider(Method handleMethod, Class<?> parameterType, Annotation[] parameterAnnotations) {
        return parameterProvider;
    }
}
