package org.openlumify.web.parameterProviders;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.vertexium.Authorizations;
import org.openlumify.core.config.Configuration;
import org.openlumify.core.model.user.AuthorizationRepository;
import org.openlumify.core.model.workspace.WorkspaceRepository;
import org.openlumify.core.user.User;
import org.openlumify.web.CurrentUser;
import org.visallo.webster.HandlerChain;
import org.visallo.webster.parameterProviders.ParameterProvider;
import org.visallo.webster.parameterProviders.ParameterProviderFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

@Singleton
public class AuthorizationsParameterProviderFactory extends ParameterProviderFactory<Authorizations> {
    private final ParameterProvider<Authorizations> parameterProvider;

    @Inject
    public AuthorizationsParameterProviderFactory(
            WorkspaceRepository workspaceRepository,
            Configuration configuration,
            AuthorizationRepository authorizationRepository
    ) {
        parameterProvider = new OpenLumifyBaseParameterProvider<Authorizations>(configuration) {
            @Override
            public Authorizations getParameter(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    HandlerChain chain
            ) {
                return getAuthorizations(request, authorizationRepository, workspaceRepository);
            }
        };
    }

    public static Authorizations getAuthorizations(
            HttpServletRequest request,
            AuthorizationRepository authorizationRepository,
            WorkspaceRepository workspaceRepository
    ) {
        User user = CurrentUser.get(request);
        if (user == null) {
            return null;
        }
        String workspaceId = OpenLumifyBaseParameterProvider.getActiveWorkspaceIdOrDefault(request, workspaceRepository);
        if (workspaceId != null) {
            return authorizationRepository.getGraphAuthorizations(user, workspaceId);
        }

        return authorizationRepository.getGraphAuthorizations(user);
    }

    @Override
    public boolean isHandled(
            Method handleMethod,
            Class<? extends Authorizations> parameterType,
            Annotation[] parameterAnnotations
    ) {
        return Authorizations.class.isAssignableFrom(parameterType);
    }

    @Override
    public ParameterProvider<Authorizations> createParameterProvider(
            Method handleMethod,
            Class<?> parameterType,
            Annotation[] parameterAnnotations
    ) {
        return parameterProvider;
    }
}
