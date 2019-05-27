package org.openlumify.web.routes.product;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.core.bootstrap.InjectHelper;
import org.openlumify.core.config.Configuration;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.model.workspace.WorkspaceRepository;
import org.openlumify.core.model.workspace.product.Product;
import org.openlumify.core.model.workspace.product.WorkProductService;
import org.openlumify.core.user.User;
import org.openlumify.core.util.ClientApiConverter;
import org.openlumify.web.clientapi.model.ClientApiProducts;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;
import org.openlumify.webster.ParameterizedHandler;
import org.openlumify.webster.annotations.Handle;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class ProductAll implements ParameterizedHandler {
    private final WorkspaceRepository workspaceRepository;
    private final Configuration configuration;

    @Inject
    public ProductAll(
            WorkspaceRepository workspaceRepository,
            Configuration configuration
    ) {
        this.workspaceRepository = workspaceRepository;
        this.configuration = configuration;
    }

    @Handle
    public ClientApiProducts handle(
            @ActiveWorkspaceId String workspaceId,
            User user
    ) throws Exception {
        Collection<Product> products = workspaceRepository.findAllProductsForWorkspace(workspaceId, user);
        if (products == null) {
            throw new OpenLumifyResourceNotFoundException("Could not find products for workspace " + workspaceId);
        }

        String lastActiveProductId = workspaceRepository.getLastActiveProductId(workspaceId, user);

        List<String> types = InjectHelper.getInjectedServices(WorkProductService.class, configuration).stream()
                .map(WorkProductService::getKind)
                .collect(Collectors.toList());

        ClientApiProducts clientApiProducts = ClientApiConverter.toClientApiProducts(types, products);
        clientApiProducts.products
                .forEach(product -> product.active = product.id.equals(lastActiveProductId));
        return clientApiProducts;
    }
}
