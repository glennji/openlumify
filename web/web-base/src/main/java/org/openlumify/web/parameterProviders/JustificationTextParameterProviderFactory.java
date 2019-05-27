package org.openlumify.web.parameterProviders;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.core.config.Configuration;
import org.openlumify.core.model.properties.OpenLumifyProperties;
import org.openlumify.core.model.user.UserRepository;
import org.openlumify.web.WebConfiguration;
import org.openlumify.webster.HandlerChain;
import org.openlumify.webster.parameterProviders.ParameterProvider;
import org.openlumify.webster.parameterProviders.ParameterProviderFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

@Singleton
public class JustificationTextParameterProviderFactory extends ParameterProviderFactory<String> {
    public static final String JUSTIFICATION_TEXT = "justificationText";
    private final ParameterProvider<String> parameterProvider;

    @Inject
    public JustificationTextParameterProviderFactory(UserRepository userRepository, final Configuration configuration) {
        final boolean isJustificationRequired = WebConfiguration.justificationRequired(configuration);

        parameterProvider = new OpenLumifyBaseParameterProvider<String>(configuration) {
            @Override
            public String getParameter(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) {
                String propertyName = getOptionalParameter(request, "propertyName");
                if (propertyName != null && propertyName.length() > 0) {
                    boolean isComment = OpenLumifyProperties.COMMENT.isSameName(propertyName);
                    String sourceInfo = getOptionalParameter(request, "sourceInfo");
                    return getJustificationText(isComment, sourceInfo, request);
                } else {
                    return justificationParameter(isJustificationRequired, request);
                }
            }

            public String getJustificationText(boolean isComment, String sourceInfo, HttpServletRequest request) {
                return justificationParameter(isJustificationRequired(isComment, sourceInfo), request);
            }

            public boolean isJustificationRequired(boolean isComment, String sourceInfo) {
                return !isComment && sourceInfo == null && isJustificationRequired;
            }

            private String justificationParameter(boolean required, HttpServletRequest request) {
                return required ?
                        getRequiredParameter(request, JUSTIFICATION_TEXT) :
                        getOptionalParameter(request, JUSTIFICATION_TEXT);
            }
        };
    }

    @Override
    public boolean isHandled(Method handleMethod, Class<? extends String> parameterType, Annotation[] parameterAnnotations) {
        return getJustificationTextAnnotation(parameterAnnotations) != null;
    }

    @Override
    public ParameterProvider<String> createParameterProvider(Method handleMethod, Class<?> parameterType, Annotation[] parameterAnnotations) {
        return parameterProvider;
    }

    private static JustificationText getJustificationTextAnnotation(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof JustificationText) {
                return (JustificationText) annotation;
            }
        }
        return null;
    }
}
