package org.openlumify.web.routes.user;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.core.model.user.UserRepository;
import org.openlumify.core.model.workspace.Workspace;
import org.openlumify.core.model.workspace.WorkspaceRepository;
import org.openlumify.core.user.User;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;
import org.openlumify.web.clientapi.model.ClientApiUser;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.handlers.CSRFHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Singleton
public class MeGet implements ParameterizedHandler {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(MeGet.class);
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;

    @Inject
    public MeGet(
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository
    ) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
    }

    @Handle
    public ClientApiUser handle(
            HttpServletRequest request,
            HttpServletResponse response,
            User user
    ) throws Exception {
        ClientApiUser userMe = userRepository.toClientApiPrivate(user);
        userMe.setCsrfToken(CSRFHandler.getSavedToken(request, response, true));

        try {
            if (userMe.getCurrentWorkspaceId() != null && userMe.getCurrentWorkspaceId().length() > 0) {
                if (!workspaceRepository.hasReadPermissions(userMe.getCurrentWorkspaceId(), user)) {
                    userMe.setCurrentWorkspaceId(null);
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Failed to read user's current workspace %s", user.getCurrentWorkspaceId(), ex);
            userMe.setCurrentWorkspaceId(null);
        }

        if (userMe.getCurrentWorkspaceId() == null) {
            Iterable<Workspace> allWorkspaces = workspaceRepository.findAllForUser(user);
            Workspace workspace = null;
            if (allWorkspaces != null) {
                Map<Boolean, List<Workspace>> userWorkspaces = StreamSupport.stream(allWorkspaces.spliterator(), false)
                        .sorted(Comparator.comparing(w -> w.getDisplayTitle().toLowerCase()))
                        .collect(Collectors.partitioningBy(userWorkspace ->
                                workspaceRepository.getCreatorUserId(userWorkspace.getWorkspaceId(), user).equals(user.getUserId())));

                List<Workspace> workspaces = userWorkspaces.get(true).isEmpty() ? userWorkspaces.get(false) : userWorkspaces.get(true);
                if (!workspaces.isEmpty()) {
                    workspace = workspaces.get(0);
                }
            }

            if (workspace == null) {
                workspace = workspaceRepository.add(user);
            }

            userRepository.setCurrentWorkspace(user.getUserId(), workspace.getWorkspaceId());
            userMe.setCurrentWorkspaceId(workspace.getWorkspaceId());
            userMe.setCurrentWorkspaceName(workspace.getDisplayTitle());
        }

        return userMe;
    }
}
