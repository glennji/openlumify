package org.openlumify.vertexium.model.notification;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlumify.core.model.notification.SystemNotificationRepository;
import org.openlumify.core.model.notification.SystemNotificationRepositoryTestBase;
import org.openlumify.core.model.user.AuthorizationRepository;

@RunWith(MockitoJUnitRunner.class)
public class VertexiumSystemNotificationRepositoryTest extends SystemNotificationRepositoryTestBase {
    private VertexiumSystemNotificationRepository systemNotificationRepository;

    @Before
    public void before() throws Exception {
        super.before();
        systemNotificationRepository = new VertexiumSystemNotificationRepository(
                getGraph(),
                getGraphRepository(),
                getGraphAuthorizationRepository(),
                getUserRepository()
        ) {
            @Override
            protected AuthorizationRepository getAuthorizationRepository() {
                return VertexiumSystemNotificationRepositoryTest.this.getAuthorizationRepository();
            }
        };
    }

    @Override
    protected SystemNotificationRepository getSystemNotificationRepository() {
        return systemNotificationRepository;
    }
}
