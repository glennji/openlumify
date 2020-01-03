package org.openlumify.web.routes.workspace;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.openlumify.core.model.workspace.WorkspaceUndoHelper;
import org.openlumify.core.user.User;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;
import org.openlumify.web.clientapi.model.ClientApiUndoItem;
import org.openlumify.web.clientapi.model.ClientApiWorkspaceUndoResponse;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;

import java.util.Arrays;

@Singleton
public class WorkspaceUndo implements ParameterizedHandler {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(WorkspaceUndo.class);
    private final WorkspaceUndoHelper workspaceUndoHelper;

    @Inject
    public WorkspaceUndo(
            final WorkspaceUndoHelper workspaceUndoHelper
    ) {
        this.workspaceUndoHelper = workspaceUndoHelper;
    }

    @Handle
    public ClientApiWorkspaceUndoResponse handle(
            @Required(name = "undoData") ClientApiUndoItem[] undoData,
            @ActiveWorkspaceId String workspaceId,
            User user,
            Authorizations authorizations
    ) throws Exception {
        LOGGER.debug("undoing:\n%s", Joiner.on("\n").join(undoData));
        ClientApiWorkspaceUndoResponse workspaceUndoResponse = new ClientApiWorkspaceUndoResponse();
        workspaceUndoHelper.undo(Arrays.asList(undoData), workspaceUndoResponse, workspaceId, user, authorizations);
        LOGGER.debug("undoing results: %s", workspaceUndoResponse);
        return workspaceUndoResponse;
    }
}
