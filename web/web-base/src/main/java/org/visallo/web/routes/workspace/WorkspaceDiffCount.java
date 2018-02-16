package org.visallo.web.routes.workspace;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.formula.FormulaEvaluator;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.ClientApiWorkspaceDiff;
import org.visallo.web.clientapi.model.ClientApiWorkspaceDiffCount;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Optional;

@Singleton
public class WorkspaceDiffCount implements ParameterizedHandler {
    private final WorkspaceRepository workspaceRepository;

    @Inject
    public WorkspaceDiffCount(final WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    @Handle
    public ClientApiWorkspaceDiffCount handle(
            @Optional(name = "query") String query,
            @ActiveWorkspaceId String workspaceId,
            User user
    ) {
        return this.workspaceRepository.getDiffCount(query, workspaceId, user);
    }
}
