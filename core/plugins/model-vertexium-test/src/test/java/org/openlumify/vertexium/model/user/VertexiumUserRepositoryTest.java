package org.openlumify.vertexium.model.user;

import org.junit.Test;
import org.openlumify.core.model.user.AuthorizationRepository;
import org.openlumify.core.model.user.UserListener;
import org.openlumify.core.model.user.UserRepository;
import org.openlumify.core.util.OpenLumifyInMemoryTestBase;

import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class VertexiumUserRepositoryTest extends OpenLumifyInMemoryTestBase {
    private VertexiumUserRepository vertexiumUserRepository;

    @Override
    protected UserRepository getUserRepository() {
        if (vertexiumUserRepository != null) {
            return vertexiumUserRepository;
        }
        vertexiumUserRepository = new VertexiumUserRepository(
                getConfiguration(),
                getGraphAuthorizationRepository(),
                getGraph(),
                getOntologyRepository(),
                getUserSessionCounterRepository(),
                getLockRepository(),
                getAuthorizationRepository(),
                getPrivilegeRepository()
        ) {
            @Override
            protected Collection<UserListener> getUserListeners() {
                return VertexiumUserRepositoryTest.this.getUserListeners();
            }

            @Override
            protected AuthorizationRepository getAuthorizationRepository() {
                return VertexiumUserRepositoryTest.this.getAuthorizationRepository();
            }
        };
        return vertexiumUserRepository;
    }

    @Test
    public void testFindOrAddUser() {
        getUserRepository().findOrAddUser("12345", "testUser", null, "testPassword");

        VertexiumUser vertexiumUser = (VertexiumUser) vertexiumUserRepository.findByUsername("12345");
        assertEquals("testUser", vertexiumUser.getDisplayName());
    }
}
