package org.openlumify.web.routes.product;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Required;
import org.openlumify.core.model.workspace.WorkspaceRepository;
import org.openlumify.core.user.User;
import org.openlumify.web.OpenLumifyResponse;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;

@Singleton
public class ProductPreview implements ParameterizedHandler {
    private final WorkspaceRepository workspaceRepository;

    @Inject
    public ProductPreview(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }


    @Handle
    public void handle(
            @Required(name = "productId") String productId,
            @ActiveWorkspaceId String workspaceId,
            User user,
            OpenLumifyResponse response
    ) throws Exception {
        try (InputStream preview = workspaceRepository.getProductPreviewById(workspaceId, productId, user)) {
            if (preview == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            } else {
                response.write(preview);
            }
        }
    }
}
