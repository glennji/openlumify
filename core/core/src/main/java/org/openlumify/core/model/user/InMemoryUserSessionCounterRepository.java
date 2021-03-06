package org.openlumify.core.model.user;

import com.google.common.collect.HashBasedTable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.core.time.TimeRepository;

import java.util.Map;

@Singleton
public class InMemoryUserSessionCounterRepository extends MapUserSessionCounterRepositoryBase {
    private final HashBasedTable<String, String, SessionData> sessionDatas = HashBasedTable.create();

    @Inject
    public InMemoryUserSessionCounterRepository(TimeRepository timeRepository) {
        super(timeRepository);
    }

    @Override
    protected void put(String userId, String sessionId, SessionData sessionData) {
        sessionDatas.put(userId, sessionId, sessionData);
    }

    @Override
    protected void remove(String userId, String sessionId) {
        sessionDatas.remove(userId, sessionId);
    }

    @Override
    protected Map<String, SessionData> getRow(String userId) {
        return sessionDatas.row(userId);
    }
}
