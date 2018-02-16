package org.visallo.core.model.workspace;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.vertexium.*;
import org.vertexium.util.IterableUtils;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.core.util.SandboxStatusUtil;
import org.visallo.web.clientapi.model.*;

import java.util.*;

@Singleton
public class WorkspaceDiffHelper {
    private static final int MAX_EDGES_TO_INCLUDE = 10;
    private final Graph graph;

    @Inject
    public WorkspaceDiffHelper(Graph graph) {
        this.graph = graph;
    }

    public ClientApiWorkspaceDiff diff(
            String workspaceId,
            Iterable<Vertex> vertices,
            Iterable<Edge> edges,
            Authorizations authorizations
    ) {
        ClientApiWorkspaceDiff diff = new ClientApiWorkspaceDiff();
        List<ClientApiWorkspaceDiff.Item> items = new ArrayList<>();

        Set<String> vertexEdgeIds = new HashSet<>();
        for (Vertex vertex : vertices) {
            SandboxStatus status = SandboxStatusUtil.getSandboxStatus(vertex, workspaceId);
            boolean deleted = vertex.isHidden(authorizations);
            int count = vertex.getEdgeCount(Direction.BOTH, authorizations);
            ClientApiVertex vertexApi;
            if (count <= MAX_EDGES_TO_INCLUDE) {
                vertexApi = ClientApiConverter.toClientApiVertex(vertex, workspaceId, null, true, authorizations);
                if (vertexApi.getEdgeInfos() != null) {
                    for (ClientApiEdgeInfo info : vertexApi.getEdgeInfos()) {
                        vertexEdgeIds.add(info.getEdgeId());
                    }
                }
            } else {
                vertexApi = ClientApiConverter.toClientApiVertex(vertex, workspaceId, authorizations);
            }
            items.add(new ClientApiWorkspaceDiff.VertexItem(vertexApi, count, status, deleted));
        }

        Set<String> edgeVertexIds = new HashSet<>();
        for (Edge edge : edges) {
            SandboxStatus status = SandboxStatusUtil.getSandboxStatus(edge, workspaceId);
            boolean deleted = edge.isHidden(authorizations);
            edgeVertexIds.add(edge.getVertexId(Direction.OUT));
            edgeVertexIds.add(edge.getVertexId(Direction.IN));
            ClientApiEdge edgeApi = ClientApiConverter.toClientApiEdge(edge, workspaceId);
            items.add(new ClientApiWorkspaceDiff.EdgeItem(edgeApi, status, deleted));
        }

        if (vertexEdgeIds.size() > 0) {
            for (Edge edge : graph.getEdges(vertexEdgeIds, FetchHint.ALL_INCLUDING_HIDDEN, authorizations)) {
                ClientApiEdge edgeApi = ClientApiConverter.toClientApiEdge(edge, workspaceId);
                SandboxStatus status = SandboxStatusUtil.getSandboxStatus(edge, workspaceId);
                boolean deleted = edge.isHidden(authorizations);
                diff.setVertexEdge(new ClientApiWorkspaceDiff.EdgeItem(edgeApi, status, deleted));
            }
        }

        if (edgeVertexIds.size() > 0) {
            for (Vertex vertex : graph.getVertices(edgeVertexIds, FetchHint.ALL_INCLUDING_HIDDEN, authorizations)) {
                ClientApiVertex vertexApi = ClientApiConverter.toClientApiVertex(vertex, workspaceId, authorizations);
                SandboxStatus status = SandboxStatusUtil.getSandboxStatus(vertex, workspaceId);
                boolean deleted = vertex.isHidden(authorizations);
                diff.setEdgeVertex(new ClientApiWorkspaceDiff.VertexItem(vertexApi, null, status, deleted));
            }
        }

        diff.addAll(items);

        return diff;
    }


//    @Traced
//    protected int diffEdge(
//            ListeningExecutorService service,
//            FutureCallback<ClientApiWorkspaceDiff.Item> callback,
//            Workspace workspace,
//            Edge edge,
//            Authorizations hiddenAuthorizations
//    ) {
//        SandboxStatus sandboxStatus = SandboxStatusUtil.getSandboxStatus(edge, workspace.getWorkspaceId());
//        boolean isPrivateChange = sandboxStatus != SandboxStatus.PUBLIC;
//        boolean isPublicDelete = WorkspaceDiffHelper.isPublicDelete(edge, hiddenAuthorizations);
//        int number = 0;
//        if (isPrivateChange || isPublicDelete) {
//            number++;
////            callback.onSuccess(createWorkspaceDiffEdgeItem(edge, sandboxStatus, isPublicDelete));
//            Futures.addCallback(service.submit(() ->
//                createWorkspaceDiffEdgeItem(edge, sandboxStatus, isPublicDelete)),
//                callback);
//        }
//
//        // don't report properties individually when deleting the edge
////        if (!isPublicDelete) {
////            number += diffProperties(service, callback, workspace, edge, hiddenAuthorizations);
////        }
//        return number;
//    }

    public static boolean isPublicDelete(Edge edge, Authorizations authorizations) {
        return edge.isHidden(authorizations);
    }

    public static boolean isPublicDelete(Vertex vertex, Authorizations authorizations) {
        return vertex.isHidden(authorizations);
    }

    public static boolean isPublicDelete(Property property, Authorizations authorizations) {
        return property.isHidden(authorizations);
    }


//    @Traced
//    protected int diffProperties(
//            ListeningExecutorService service,
//            FutureCallback<ClientApiWorkspaceDiff.Item> callback,
//            Workspace workspace,
//            Element element,
//            Authorizations hiddenAuthorizations
//    ) {
//        List<Property> properties = toList(element.getProperties());
//        SandboxStatus[] propertyStatuses = SandboxStatusUtil.getPropertySandboxStatuses(
//                properties,
//                workspace.getWorkspaceId()
//        );
//        int number = 0;
//        for (int i = 0; i < properties.size(); i++) {
//            Property property = properties.get(i);
//            boolean isPrivateChange = propertyStatuses[i] != SandboxStatus.PUBLIC;
//            boolean isPublicDelete = WorkspaceDiffHelper.isPublicDelete(property, hiddenAuthorizations);
//            if (isPrivateChange || isPublicDelete) {
//                Property existingProperty = null;
//                if (isPublicDelete && isPublicPropertyEdited(properties, propertyStatuses, property)) {
//                    continue;
//                } else if (isPrivateChange) {
//                    existingProperty = findExistingProperty(properties, propertyStatuses, property);
//                }
//                final Property e = existingProperty;
//                final SandboxStatus status = propertyStatuses[i];
//                number++;
//                Futures.addCallback(service.submit(() -> createWorkspaceDiffPropertyItem(
//                        element,
//                        property,
//                        e,
//                        status,
//                        isPublicDelete
//                )), callback);
//            }
//        }
//        return number;
//    }

//    private ClientApiWorkspaceDiff.PropertyItem createWorkspaceDiffPropertyItem(
//            Element element,
//            Property workspaceProperty,
//            Property existingProperty,
//            SandboxStatus sandboxStatus,
//            boolean deleted
//    ) {
//        JsonNode oldData = null;
//        if (existingProperty != null) {
//            oldData = JSONUtil.toJsonNode(JsonSerializer.toJsonProperty(existingProperty));
//        }
//        JsonNode newData = JSONUtil.toJsonNode(JsonSerializer.toJsonProperty(workspaceProperty));
//
//        ElementType type = ElementType.getTypeFromElement(element);
//        if (type.equals(ElementType.VERTEX)) {
//            return new ClientApiWorkspaceDiff.PropertyItem(
//                    type.name().toLowerCase(),
//                    element.getId(),
//                    VisalloProperties.CONCEPT_TYPE.getPropertyValue(element),
//                    workspaceProperty.getName(),
//                    workspaceProperty.getKey(),
//                    oldData,
//                    newData,
//                    sandboxStatus,
//                    deleted,
//                    workspaceProperty.getVisibility().getVisibilityString()
//            );
//        } else {
//            return new ClientApiWorkspaceDiff.PropertyItem(
//                    type.name().toLowerCase(),
//                    element.getId(),
//                    ((Edge) element).getLabel(),
//                    ((Edge) element).getVertexId(Direction.OUT),
//                    ((Edge) element).getVertexId(Direction.IN),
//                    workspaceProperty.getName(),
//                    workspaceProperty.getKey(),
//                    oldData,
//                    newData,
//                    sandboxStatus,
//                    deleted,
//                    workspaceProperty.getVisibility().getVisibilityString()
//            );
//        }
//    }

//    private Property findExistingProperty(
//            List<Property> properties,
//            SandboxStatus[] propertyStatuses,
//            Property workspaceProperty
//    ) {
//        for (int i = 0; i < properties.size(); i++) {
//            Property property = properties.get(i);
//            if (property.getName().equals(workspaceProperty.getName())
//                    && property.getKey().equals(workspaceProperty.getKey())
//                    && propertyStatuses[i] == SandboxStatus.PUBLIC) {
//                return property;
//            }
//        }
//        return null;
//    }
//

    public static boolean isPublicPropertyEdited(
            List<Property> properties,
            SandboxStatus[] propertyStatuses,
            Property workspaceProperty
    ) {
        for (int i = 0; i < properties.size(); i++) {
            Property property = properties.get(i);
            if (property.getName().equals(workspaceProperty.getName())
                    && property.getKey().equals(workspaceProperty.getKey())
                    && propertyStatuses[i] == SandboxStatus.PUBLIC_CHANGED) {
                return true;
            }
        }
        return false;
    }

    class TitleOperation {
        public String vertexId;
        public String title;

        public TitleOperation(String vertexId, String title) {
            this.vertexId = vertexId;
            this.title = title;
        }
    }

}
