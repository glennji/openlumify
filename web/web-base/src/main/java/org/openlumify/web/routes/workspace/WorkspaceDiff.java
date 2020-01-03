package org.openlumify.web.routes.workspace;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.formula.FormulaEvaluator;
import org.openlumify.core.model.workspace.Workspace;
import org.openlumify.core.model.workspace.WorkspaceRepository;
import org.openlumify.core.user.User;
import org.openlumify.web.clientapi.model.ClientApiWorkspaceDiff;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;

@Singleton
public class WorkspaceDiff implements ParameterizedHandler {
    private final WorkspaceRepository workspaceRepository;

    @Inject
    public WorkspaceDiff(final WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    @Handle
    public ClientApiWorkspaceDiff handle(
            @ActiveWorkspaceId String workspaceId,
            FormulaEvaluator.UserContext userContext,
            User user
    ) throws Exception {
        Workspace workspace = workspaceRepository.findById(workspaceId, true, user);
        if (workspace == null) {
            throw new OpenLumifyResourceNotFoundException("Cannot find workspace: " + workspaceId);
        }

        return this.workspaceRepository.getDiff(workspace, user, userContext);
    }
}
