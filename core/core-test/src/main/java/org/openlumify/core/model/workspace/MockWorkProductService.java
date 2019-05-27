package org.openlumify.core.model.workspace;

import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.openlumify.core.model.workspace.product.GetExtendedDataParams;
import org.openlumify.core.model.workspace.product.WorkProductExtendedData;
import org.openlumify.core.model.workspace.product.WorkProductService;
import org.openlumify.core.user.User;

public class MockWorkProductService implements WorkProductService {
    public static final String KIND = "org.openlumify.core.model.workspace.MockWorkProduct";

    @Override
    public WorkProductExtendedData getExtendedData(
            Graph graph,
            Vertex workspaceVertex,
            Vertex productVertex,
            GetExtendedDataParams params,
            User user,
            Authorizations authorizations
    ) {
        return new WorkProductExtendedData();
    }

    @Override
    public String getKind() {
        return KIND;
    }
}
