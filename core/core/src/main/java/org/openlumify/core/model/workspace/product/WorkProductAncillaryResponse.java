package org.openlumify.core.model.workspace.product;

import org.openlumify.web.clientapi.model.ClientApiObject;
import org.openlumify.web.clientapi.model.ClientApiVertex;

public class WorkProductAncillaryResponse implements ClientApiObject {
    private ClientApiVertex vertex;
    private WorkProductVertex productVertex;

    public WorkProductAncillaryResponse(ClientApiVertex vertex, WorkProductVertex productVertex) {
        this.vertex = vertex;
        this.productVertex = productVertex;
    }

    public ClientApiVertex getVertex() {
        return vertex;
    }

    public WorkProductVertex getProductVertex() {
        return productVertex;
    }
}
