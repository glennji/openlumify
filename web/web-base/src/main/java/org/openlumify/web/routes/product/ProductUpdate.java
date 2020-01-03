package org.openlumify.web.routes.product;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.json.JSONObject;
import org.openlumify.core.model.workspace.WorkspaceHelper;
import org.openlumify.core.model.workspace.WorkspaceRepository;
import org.openlumify.core.model.workspace.product.Product;
import org.openlumify.core.user.User;
import org.openlumify.core.util.ClientApiConverter;
import org.openlumify.web.clientapi.model.ClientApiProduct;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;
import org.openlumify.web.parameterProviders.SourceGuid;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Optional;

@Singleton
public class ProductUpdate implements ParameterizedHandler {
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceHelper workspaceHelper;

    @Inject
    public ProductUpdate(
            final WorkspaceRepository workspaceRepository,
            final WorkspaceHelper workspaceHelper
    ) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceHelper = workspaceHelper;
    }

    @Handle
    public ClientApiProduct handle(
            @Optional(name = "productId") String productId,
            @Optional(name = "title") String title,
            @Optional(name = "kind") String kind,
            @Optional(name = "params") String paramsStr,
            @Optional(name = "preview") String previewDataUrl,
            @ActiveWorkspaceId String workspaceId,
            @SourceGuid String sourceGuid,
            User user
    ) throws Exception {
        JSONObject params = paramsStr == null ? new JSONObject() : new JSONObject(paramsStr);
        Product product;
        if (previewDataUrl == null) {
            if (params.has("broadcastOptions")) {
                params.getJSONObject("broadcastOptions").put("sourceGuid", sourceGuid);
            }
            product = workspaceRepository.addOrUpdateProduct(workspaceId, productId, title, kind, params, user);
        } else {
            product = workspaceRepository.updateProductPreview(workspaceId, productId, previewDataUrl, user);
        }
        return ClientApiConverter.toClientApiProduct(product);
    }

}
