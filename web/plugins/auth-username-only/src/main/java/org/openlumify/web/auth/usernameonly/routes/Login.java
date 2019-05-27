package org.openlumify.web.auth.usernameonly.routes;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.json.JSONObject;
import org.openlumify.core.model.user.AuthorizationContext;
import org.openlumify.core.model.user.UserNameAuthorizationContext;
import org.openlumify.core.model.user.UserRepository;
import org.openlumify.core.security.AuditService;
import org.openlumify.core.user.User;
import org.openlumify.web.CurrentUser;
import org.openlumify.web.util.RemoteAddressUtil;
import org.openlumify.webster.ParameterizedHandler;
import org.openlumify.webster.annotations.Handle;
import org.openlumify.webster.utils.UrlUtils;

import javax.servlet.http.HttpServletRequest;

@Singleton
public class Login implements ParameterizedHandler {
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Inject
    public Login(
            UserRepository userRepository,
            AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Handle
    public JSONObject handle(HttpServletRequest request) {
        final String username = UrlUtils.urlDecode(request.getParameter("username")).trim().toLowerCase();
        User user = userRepository.findByUsername(username);
        if (user == null) {
            // For form based authentication, username and displayName will be the same
            String randomPassword = UserRepository.createRandomPassword();
            user = userRepository.findOrAddUser(
                    username,
                    username,
                    null,
                    randomPassword
            );
        }

        AuthorizationContext authorizationContext = new UserNameAuthorizationContext(
                username,
                RemoteAddressUtil.getClientIpAddr(request)
        );
        userRepository.updateUser(user, authorizationContext);

        CurrentUser.set(request, user);
        auditService.auditLogin(user);
        JSONObject json = new JSONObject();
        json.put("status", "OK");
        return json;
    }
}
