package org.openlumify.vertexium.model.longRunningProcess;

import org.openlumify.core.model.longRunningProcess.LongRunningProcessRepository;
import org.openlumify.core.model.longRunningProcess.LongRunningProcessRepositoryTestBase;

public class VertexiumLongRunningProcessRepositoryTest extends LongRunningProcessRepositoryTestBase {
    @Override
    public LongRunningProcessRepository getLongRunningProcessRepository() {
        return new VertexiumLongRunningProcessRepository(
                getGraphRepository(),
                getGraphAuthorizationRepository(),
                getUserRepository(),
                getWorkQueueRepository(),
                getGraph(),
                getAuthorizationRepository()
        );
    }
}