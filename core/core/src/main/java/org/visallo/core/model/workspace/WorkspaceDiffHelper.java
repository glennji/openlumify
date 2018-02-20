package org.visallo.core.model.workspace;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.vertexium.*;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.core.util.SandboxStatusUtil;
import org.visallo.web.clientapi.model.*;
import org.visallo.web.clientapi.model.ClientApiWorkspaceDiff.PropertyItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.visallo.core.util.StreamUtil.stream;

@Singleton
public class WorkspaceDiffHelper {
    private static final int MAX_EDGES_TO_INCLUDE = 10;
    private final Graph graph;

    @Inject
    public WorkspaceDiffHelper(Graph graph) {
        this.graph = graph;
    }

    public List<PropertyItem> diffElementProperties(Element element, String workspaceId, Authorizations authorizations) {
        List<PropertyItem> items = new ArrayList<>();
        List<Property> properties = stream(element.getProperties()).collect(Collectors.toList());
        SandboxStatus[] propertyStatuses = SandboxStatusUtil.getPropertySandboxStatuses(
                properties,
                workspaceId
        );
        String elementId = element.getId();
        String elementType = (element instanceof Vertex ? ElementType.VERTEX : ElementType.EDGE).name();

        for (int i = 0; i < properties.size(); i++) {
            Property property = properties.get(i);
            boolean isPrivateChange = propertyStatuses[i] != SandboxStatus.PUBLIC;
            boolean isPublicDelete = WorkspaceDiffHelper.isPublicDelete(property, authorizations);
            if (isPrivateChange || isPublicDelete) {
                Property previousProperty = null;
                if (isPublicDelete && isPublicPropertyEdited(properties, propertyStatuses, property)) {
                    continue;
                } else if (isPrivateChange) {
                    previousProperty = findExistingProperty(properties, propertyStatuses, property);
                }

                ClientApiProperty propertyApi = ClientApiConverter.toClientApiProperty(property);
                ClientApiProperty previousPropertyApi = null;

                if (previousProperty != null) {
                    previousPropertyApi = ClientApiConverter.toClientApiProperty(previousProperty);
                }

                items.add(new PropertyItem(elementId, elementType, propertyApi, previousPropertyApi, propertyStatuses[i], isPublicDelete));
            }
        }

        return items;
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

    public static boolean isPublicDelete(Edge edge, Authorizations authorizations) {
        return edge.isHidden(authorizations);
    }

    public static boolean isPublicDelete(Vertex vertex, Authorizations authorizations) {
        return vertex.isHidden(authorizations);
    }

    public static boolean isPublicDelete(Property property, Authorizations authorizations) {
        return property.isHidden(authorizations);
    }

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

    private Property findExistingProperty(
            List<Property> properties,
            SandboxStatus[] propertyStatuses,
            Property workspaceProperty
    ) {
        for (int i = 0; i < properties.size(); i++) {
            Property property = properties.get(i);
            if (property.getName().equals(workspaceProperty.getName())
                    && property.getKey().equals(workspaceProperty.getKey())
                    && propertyStatuses[i] == SandboxStatus.PUBLIC) {
                return property;
            }
        }
        return null;
    }
}
