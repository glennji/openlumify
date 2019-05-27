package org.openlumify.web.auth.usernamepassword.routes;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.webster.ParameterizedHandler;
import org.openlumify.webster.annotations.ContentType;
import org.openlumify.webster.annotations.Handle;
import org.openlumify.webster.annotations.Required;
import org.openlumify.core.config.Configuration;
import org.openlumify.core.exception.OpenLumifyAccessDeniedException;
import org.openlumify.core.model.user.UserRepository;
import org.openlumify.core.user.User;
import org.openlumify.web.auth.usernamepassword.ForgotPasswordConfiguration;
import org.openlumify.web.auth.usernamepassword.UsernamePasswordWebAppPlugin;
import org.openlumify.web.parameterProviders.BaseUrl;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class LookupToken implements ParameterizedHandler {
    public static final String TOKEN_PARAMETER_NAME = "token";
    private static final String TEMPLATE_PATH = "/org/openlumify/web/auth/usernamepassword/templates";
    private static final String TEMPLATE_NAME = "changePasswordWithToken";
    private final UserRepository userRepository;
    private ForgotPasswordConfiguration forgotPasswordConfiguration;

    @Inject
    public LookupToken(UserRepository userRepository, Configuration configuration) {
        this.userRepository = userRepository;
        forgotPasswordConfiguration = new ForgotPasswordConfiguration();
        configuration.setConfigurables(forgotPasswordConfiguration, ForgotPasswordConfiguration.CONFIGURATION_PREFIX);
    }

    @Handle
    @ContentType("text/html")
    public String handle(
            @BaseUrl String baseUrl,
            @Required(name = TOKEN_PARAMETER_NAME) String token
    ) throws Exception {
        User user = userRepository.findByPasswordResetToken(token);
        if (user == null) {
            throw new OpenLumifyAccessDeniedException("invalid token", null, null);
        }

        Date now = new Date();
        if (!user.getPasswordResetTokenExpirationDate().after(now)) {
            throw new OpenLumifyAccessDeniedException("expired token", user, null);
        }

        return getHtml(baseUrl, token);
    }

    private String getHtml(String baseUrl, String token) throws IOException {
        Map<String, String> context = new HashMap<>();
        context.put("formAction", baseUrl + UsernamePasswordWebAppPlugin.CHANGE_PASSWORD_ROUTE);
        context.put("tokenParameterName", ChangePassword.TOKEN_PARAMETER_NAME);
        context.put("token", token);
        context.put("newPasswordLabel", forgotPasswordConfiguration.getNewPasswordLabel());
        context.put("newPasswordParameterName", ChangePassword.NEW_PASSWORD_PARAMETER_NAME);
        context.put("newPasswordConfirmationLabel", forgotPasswordConfiguration.getNewPasswordConfirmationLabel());
        context.put("newPasswordConfirmationParameterName", ChangePassword.NEW_PASSWORD_CONFIRMATION_PARAMETER_NAME);
        TemplateLoader templateLoader = new ClassPathTemplateLoader(TEMPLATE_PATH);
        Handlebars handlebars = new Handlebars(templateLoader);
        Template template = handlebars.compile(TEMPLATE_NAME);
        return template.apply(context);
    }
}
