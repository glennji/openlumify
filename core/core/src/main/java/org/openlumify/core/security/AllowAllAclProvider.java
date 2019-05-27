package org.openlumify.core.security;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.vertexium.Element;
import org.vertexium.Graph;
import org.openlumify.core.model.ontology.Ontology;
import org.openlumify.core.model.ontology.OntologyElement;
import org.openlumify.core.model.ontology.OntologyRepository;
import org.openlumify.core.model.user.PrivilegeRepository;
import org.openlumify.core.model.user.UserRepository;
import org.openlumify.core.user.User;
import org.openlumify.web.clientapi.model.ClientApiElement;

@Singleton
public class AllowAllAclProvider extends ACLProvider {
    @Inject
    public AllowAllAclProvider(
            Graph graph,
            UserRepository userRepository,
            OntologyRepository ontologyRepository,
            PrivilegeRepository privilegeRepository
    ) {
        super(graph, userRepository, ontologyRepository, privilegeRepository);
    }

    @Override
    public boolean canDeleteElement(Element element, OntologyElement ontologyElement, Ontology ontology, User user, String workspaceId) {
        return true;
    }

    @Override
    public boolean canDeleteElement(ClientApiElement clientApiElement, OntologyElement ontologyElement, Ontology ontology, User user, String workspaceId) {
        return true;
    }

    @Override
    public boolean canDeleteProperty(Element element, OntologyElement ontologyElement, String propertyKey, String propertyName, Ontology ontology, User user, String workspaceId) {
        return true;
    }

    @Override
    public boolean canDeleteProperty(ClientApiElement clientApiElement, OntologyElement ontologyElement, String propertyKey, String propertyName, Ontology ontology, User user, String workspaceId) {
        return true;
    }

    @Override
    public boolean canUpdateElement(Element element, OntologyElement ontologyElement, Ontology ontology, User user, String workspaceId) {
        return true;
    }

    @Override
    public boolean canUpdateElement(ClientApiElement clientApiElement, OntologyElement ontologyElement, Ontology ontology, User user, String workspaceId) {
        return true;
    }

    @Override
    public boolean canUpdateProperty(Element element, OntologyElement ontologyElement, String propertyKey, String propertyName, Ontology ontology, User user, String workspaceId) {
        return true;
    }

    @Override
    public boolean canUpdateProperty(ClientApiElement clientApiElement, OntologyElement ontologyElement, String propertyKey, String propertyName, Ontology ontology, User user, String workspaceId) {
        return true;
    }

    @Override
    public boolean canAddProperty(Element element, OntologyElement ontologyElement, String propertyKey, String propertyName, Ontology ontology, User user, String workspaceId) {
        return true;
    }

    @Override
    public boolean canAddProperty(ClientApiElement clientApiElement, OntologyElement ontologyElement, String propertyKey, String propertyName, Ontology ontology, User user, String workspaceId) {
        return true;
    }
}
