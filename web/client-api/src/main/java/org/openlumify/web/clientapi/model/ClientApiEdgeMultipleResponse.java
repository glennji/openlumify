package org.openlumify.web.clientapi.model;

import org.openlumify.web.clientapi.util.ClientApiConverter;

import java.util.ArrayList;
import java.util.List;

public class ClientApiEdgeMultipleResponse implements ClientApiObject {
    private List<ClientApiEdge> edges = new ArrayList<ClientApiEdge>();

    public List<ClientApiEdge> getEdges() {
        return edges;
    }

    @Override
    public String toString() {
        return ClientApiConverter.clientApiToString(this);
    }
}
