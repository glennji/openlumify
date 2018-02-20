package org.visallo.web.routes.workspace;

import com.amazonaws.services.devicefarm.model.ArgumentException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.vertexium.Authorizations;
import org.vertexium.Element;
import org.vertexium.FetchHint;
import org.vertexium.Graph;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.ClientApiWorkspaceDiff;
import org.visallo.web.clientapi.model.ClientApiWorkspaceDiff.PropertyItem;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Optional;
import org.visallo.webster.annotations.Required;

import java.util.List;

@Singleton
public class WorkspaceDiffElement implements ParameterizedHandler {
    private final WorkspaceRepository workspaceRepository;
    private final Graph graph;

    @Inject
    public WorkspaceDiffElement(final Graph graph, final WorkspaceRepository workspaceRepository) {
        this.graph = graph;
        this.workspaceRepository = workspaceRepository;
    }

    @Handle
    public List<PropertyItem> handle(
            @Optional(name = "vertexId", allowEmpty = false) String vertexId,
            @Optional(name = "edgeId", allowEmpty = false) String edgeId,
            @ActiveWorkspaceId String workspaceId,
            Authorizations authorizations,
            User user
    ) {

        Element element = null;
        if (vertexId != null) {
            element = graph.getVertex(vertexId, FetchHint.ALL_INCLUDING_HIDDEN, authorizations);
        } else if (edgeId != null) {
            element = graph.getEdge(edgeId, FetchHint.ALL_INCLUDING_HIDDEN, authorizations);
        } else {
            throw new ArgumentException("Must pass either vertexId or edgeId");
        }

        return this.workspaceRepository.getElementPropertyDiffs(element, workspaceId, user);
    }
}
