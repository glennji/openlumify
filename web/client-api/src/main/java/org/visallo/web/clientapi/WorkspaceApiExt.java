package org.visallo.web.clientapi;

import org.visallo.web.clientapi.codegen.ApiException;
import org.visallo.web.clientapi.codegen.WorkspaceApi;
import org.visallo.web.clientapi.model.*;

import java.util.ArrayList;
import java.util.List;

public class WorkspaceApiExt extends WorkspaceApi {
    public void update(ClientApiWorkspaceUpdateData updateData) throws ApiException {
        update(ApiInvoker.serialize(updateData));
    }

    public ClientApiWorkspacePublishResponse publishAll(List<ClientApiWorkspaceDiff.Item> diffItems) throws ApiException {
        List<ClientApiPublishItem> publishItems = new ArrayList<ClientApiPublishItem>();
        for (ClientApiWorkspaceDiff.Item diffItem : diffItems) {
            publishItems.add(workspaceDiffItemToPublishItem(diffItem));
        }
        return publish(publishItems);
    }

    public ClientApiWorkspacePublishResponse publish(List<ClientApiPublishItem> publishItems) throws ApiException {
        return publish(ApiInvoker.serialize(publishItems));
    }

    public ClientApiPublishItem workspaceDiffItemToPublishItem(ClientApiWorkspaceDiff.Item workspaceDiffItem) {
        if (workspaceDiffItem instanceof ClientApiWorkspaceDiff.VertexItem) {
            ClientApiWorkspaceDiff.VertexItem vertexDiffItem = (ClientApiWorkspaceDiff.VertexItem) workspaceDiffItem;
            ClientApiVertexPublishItem publishItem = new ClientApiVertexPublishItem();
            publishItem.setAction(ClientApiPublishItem.Action.ADD_OR_UPDATE);
            publishItem.setVertexId(vertexDiffItem.getVertex().getId());
            return publishItem;
        } else if (workspaceDiffItem instanceof ClientApiWorkspaceDiff.PropertyItem) {
            ClientApiWorkspaceDiff.PropertyItem propertyDiffItem = (ClientApiWorkspaceDiff.PropertyItem) workspaceDiffItem;
            ClientApiPropertyPublishItem publishItem = new ClientApiPropertyPublishItem();
            publishItem.setElementId(propertyDiffItem.getElementId());
            publishItem.setKey(propertyDiffItem.getProperty().getKey());
            publishItem.setName(propertyDiffItem.getProperty().getName());
            publishItem.setVisibilityString(propertyDiffItem.getProperty().getVisibilitySource());
            return publishItem;
        } else if (workspaceDiffItem instanceof ClientApiWorkspaceDiff.EdgeItem) {
            ClientApiWorkspaceDiff.EdgeItem edgeDiffItem = (ClientApiWorkspaceDiff.EdgeItem) workspaceDiffItem;
            ClientApiRelationshipPublishItem publishItem = new ClientApiRelationshipPublishItem();
            publishItem.setEdgeId(edgeDiffItem.getEdge().getId());
            return publishItem;
        } else {
            throw new VisalloClientApiException("Unhandled WorkspaceDiffItem type: " + workspaceDiffItem.getType());
        }
    }

    public ClientApiWorkspaceUndoResponse undoAll(List<ClientApiWorkspaceDiff.Item> diffItems) throws ApiException {
        List<ClientApiUndoItem> undoItems = new ArrayList<ClientApiUndoItem>();
        for (ClientApiWorkspaceDiff.Item diffItem : diffItems) {
            undoItems.add(workspaceDiffItemToUndoItem(diffItem));
        }
        return undo(undoItems);
    }

    public ClientApiWorkspaceUndoResponse undo(List<ClientApiUndoItem> undoItems) throws ApiException {
        return undo(ApiInvoker.serialize(undoItems));
    }

    public ClientApiUndoItem workspaceDiffItemToUndoItem(ClientApiWorkspaceDiff.Item workspaceDiffItem) {
        if (workspaceDiffItem instanceof ClientApiWorkspaceDiff.VertexItem) {
            ClientApiWorkspaceDiff.VertexItem vertexDiffItem = (ClientApiWorkspaceDiff.VertexItem) workspaceDiffItem;
            ClientApiVertexUndoItem undoItem = new ClientApiVertexUndoItem();
            undoItem.setVertexId(vertexDiffItem.getVertex().getId());
            return undoItem;
        } else if (workspaceDiffItem instanceof ClientApiWorkspaceDiff.PropertyItem) {
            ClientApiWorkspaceDiff.PropertyItem propertyDiffItem = (ClientApiWorkspaceDiff.PropertyItem) workspaceDiffItem;
            ClientApiPropertyUndoItem undoItem = new ClientApiPropertyUndoItem();
            undoItem.setElementId(propertyDiffItem.getElementId());
            undoItem.setKey(propertyDiffItem.getProperty().getKey());
            undoItem.setName(propertyDiffItem.getProperty().getName());
            undoItem.setVisibilityString(propertyDiffItem.getProperty().getVisibilitySource());
            return undoItem;
        } else if (workspaceDiffItem instanceof ClientApiWorkspaceDiff.EdgeItem) {
            ClientApiWorkspaceDiff.EdgeItem edgeDiffItem = (ClientApiWorkspaceDiff.EdgeItem) workspaceDiffItem;
            ClientApiRelationshipUndoItem undoItem = new ClientApiRelationshipUndoItem();
            undoItem.setEdgeId(edgeDiffItem.getEdge().getId());
            return undoItem;
        } else {
            throw new VisalloClientApiException("Unhandled WorkspaceDiffItem type: " + workspaceDiffItem.getType());
        }
    }

    public void setUserAccess(String userId, WorkspaceAccess access) throws ApiException {
        ClientApiWorkspaceUpdateData addUser2WorkspaceUpdate = new ClientApiWorkspaceUpdateData();
        ClientApiWorkspaceUpdateData.UserUpdate addUser2Update = new ClientApiWorkspaceUpdateData.UserUpdate();
        addUser2Update.setUserId(userId);
        addUser2Update.setAccess(access);
        addUser2WorkspaceUpdate.getUserUpdates().add(addUser2Update);
        update(addUser2WorkspaceUpdate);
    }
}
