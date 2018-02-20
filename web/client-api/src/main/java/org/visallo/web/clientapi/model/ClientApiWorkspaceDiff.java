package org.visallo.web.clientapi.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.visallo.web.clientapi.util.ClientApiConverter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientApiWorkspaceDiff implements ClientApiObject {
    private long total;
    private Map<String, VertexItem> edgeVerticesById = new HashMap<String, VertexItem>();
    private Map<String, EdgeItem> vertexEdgesById = new HashMap<String, EdgeItem>();
    private List<Item> diffs = new ArrayList<Item>();

    public void addAll(List<Item> diffs) {
        this.diffs.addAll(diffs);
    }

    public List<Item> getDiffs() {
        return diffs;
    }

    public void setVertexEdge(EdgeItem edge) {
        vertexEdgesById.put(edge.getEdge().getId(), edge);
    }

    public void setEdgeVertex(VertexItem vertex) {
        edgeVerticesById.put(vertex.getVertex().getId(), vertex);
    }

    public Map<String, VertexItem> getEdgeVerticesById() {
        return edgeVerticesById;
    }

    public Map<String, EdgeItem> getVertexEdgesById() {
        return vertexEdgesById;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    @Override
    public String toString() {
        return ClientApiConverter.clientApiToString(this);
    }

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY,
            property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = VertexItem.class, name = "VertexDiffItem"),
            @JsonSubTypes.Type(value = EdgeItem.class, name = "EdgeDiffItem"),
            @JsonSubTypes.Type(value = PropertyItem.class, name = "PropertyDiffItem")
    })
    public abstract static class Item {
        private final String type;
        private final SandboxStatus sandboxStatus;
        private boolean deleted;

        protected Item(String type, SandboxStatus sandboxStatus, boolean deleted) {
            this.type = type;
            this.sandboxStatus = sandboxStatus;
            this.deleted = deleted;
        }

        public String getType() {
            return type;
        }

        public SandboxStatus getSandboxStatus() {
            return sandboxStatus;
        }

        @Override
        public String toString() {
            return ClientApiConverter.clientApiToString(this);
        }

        public boolean isDeleted() {
            return deleted;
        }
    }

    public static class EdgeItem extends Item {
        private ClientApiEdge edge;

        public EdgeItem() {
            super("EdgeDiffItem", SandboxStatus.PRIVATE, false);
        }

        public EdgeItem(ClientApiEdge edge, SandboxStatus sandboxStatus, boolean deleted) {
            super("EdgeDiffItem", sandboxStatus, deleted);
            this.edge = edge;
        }

        public ClientApiEdge getEdge() {
            return edge;
        }
    }

    public static class VertexItem extends Item {
        private ClientApiVertex vertex;
        private Integer edgeCount;

        public VertexItem() {
            super("VertexDiffItem", SandboxStatus.PRIVATE, false);
        }

        public VertexItem(
                ClientApiVertex vertex,
                Integer edgeCount,
                SandboxStatus sandboxStatus,
                boolean deleted
        ) {
            super("VertexDiffItem", sandboxStatus, deleted);
            this.vertex = vertex;
            this.edgeCount = edgeCount;
        }

        public ClientApiVertex getVertex() {
            return vertex;
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public Integer getEdgeCount() {
            return edgeCount;
        }
    }

    public static class PropertyItem extends Item {
        private String elementId;
        private String elementType;
        private ClientApiProperty property;
        private ClientApiProperty previousProperty;

        public PropertyItem() {
            super("PropertyDiffItem", SandboxStatus.PRIVATE, false);
        }

        public PropertyItem(
                String elementId,
                String elementType,
                ClientApiProperty property,
                ClientApiProperty previousProperty,
                SandboxStatus sandboxStatus,
                boolean deleted) {
            super("PropertyDiffItem", sandboxStatus, deleted);
            this.elementId = elementId;
            this.elementType = elementType;
            this.property = property;
            this.previousProperty = previousProperty;
        }

        public String getElementType() {
            return elementType;
        }

        public String getElementId() {
            return elementId;
        }

        public ClientApiProperty getProperty() {
            return property;
        }

        public ClientApiProperty getPreviousProperty() {
            return previousProperty;
        }
    }
}
