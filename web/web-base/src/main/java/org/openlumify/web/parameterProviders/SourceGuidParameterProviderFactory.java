package org.openlumify.web.parameterProviders;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.core.config.Configuration;
import org.openlumify.webster.HandlerChain;
import org.openlumify.webster.parameterProviders.ParameterProvider;
import org.openlumify.webster.parameterProviders.ParameterProviderFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

@Singleton
public class SourceGuidParameterProviderFactory extends ParameterProviderFactory<String> {
    private final ParameterProvider<String> parameterProvider;

    @Inject
    public SourceGuidParameterProviderFactory(Configuration configuration) {
        parameterProvider = new OpenLumifyBaseParameterProvider<String>(configuration) {
            @Override
            public String getParameter(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) {
                return getSourceGuid(request);
            }
        };
    }

    @Override
    public boolean isHandled(Method handleMethod, Class<? extends String> parameterType, Annotation[] parameterAnnotations) {
        return getSourceGuidAnnotation(parameterAnnotations) != null;
    }

    @Override
    public ParameterProvider<String> createParameterProvider(Method handleMethod, Class<?> parameterType, Annotation[] parameterAnnotations) {
        return parameterProvider;
    }

    private static SourceGuid getSourceGuidAnnotation(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof SourceGuid) {
                return (SourceGuid) annotation;
            }
        }
        return null;
    }
}
