package org.openlumify.web.routes.product;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Optional;
import org.visallo.webster.annotations.Required;
import org.openlumify.core.model.workspace.WorkspaceRepository;
import org.openlumify.core.model.workspace.product.GetExtendedDataParams;
import org.openlumify.core.model.workspace.product.Product;
import org.openlumify.core.user.User;
import org.openlumify.core.util.ClientApiConverter;
import org.openlumify.web.clientapi.model.ClientApiProduct;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;

@Singleton
public class ProductGet implements ParameterizedHandler {
    private final WorkspaceRepository workspaceRepository;

    @Inject
    public ProductGet(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }


    @Handle
    public ClientApiProduct handle(
            @Required(name = "productId") String productId,
            @Optional(name = "includeExtended", defaultValue = "true") boolean includeExtended,
            @Optional(name = "params") String paramsStr,
            @ActiveWorkspaceId String workspaceId,
            User user
    ) throws Exception {
        GetExtendedDataParams params = paramsStr == null
                ? new GetExtendedDataParams()
                : ClientApiConverter.toClientApi(paramsStr, GetExtendedDataParams.class);
        Product product = workspaceRepository.findProductById(workspaceId, productId, params, includeExtended, user);
        return ClientApiConverter.toClientApiProduct(product);
    }
}
