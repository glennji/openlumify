#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.auth;

import com.google.inject.Singleton;
import com.google.inject.Inject;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Required;
import org.json.JSONObject;
import org.openlumify.core.exception.OpenLumifyAccessDeniedException;
import org.openlumify.core.model.user.UserNameAuthorizationContext;
import org.openlumify.core.model.user.UserRepository;
import org.openlumify.core.user.User;
import org.openlumify.web.CurrentUser;
import org.openlumify.web.util.RemoteAddressUtil;

import javax.servlet.http.HttpServletRequest;

@Singleton
public class Login implements ParameterizedHandler {

    private final UserRepository userRepository;

    @Inject
    public Login(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Handle
    public JSONObject handle(
            HttpServletRequest request,
            @Required(name = "username") String username,
            @Required(name = "password") String password
    ) throws Exception {
        username = username.trim();
        password = password.trim();

        if (isValid(username, password)) {
            User user = findOrCreateUser(username);
            userRepository.updateUser(user, new UserNameAuthorizationContext(username, RemoteAddressUtil.getClientIpAddr(request)));
            CurrentUser.set(request, user);
            JSONObject json = new JSONObject();
            json.put("status", "OK");
            return json;
        } else {
            throw new OpenLumifyAccessDeniedException("", null, null);
        }
    }

    private User findOrCreateUser(String username) {
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
        return user;
    }

    private boolean isValid(String username, String password) {
        return username.equals(password);
    }
}
