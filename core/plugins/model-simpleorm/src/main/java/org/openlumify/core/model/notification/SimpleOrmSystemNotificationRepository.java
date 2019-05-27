package org.openlumify.core.model.notification;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.v5analytics.simpleorm.SimpleOrmSession;
import org.json.JSONObject;
import org.vertexium.Graph;
import org.openlumify.core.model.graph.GraphRepository;
import org.openlumify.core.model.user.GraphAuthorizationRepository;
import org.openlumify.core.model.user.UserRepository;
import org.openlumify.core.simpleorm.SimpleOrmContextProvider;
import org.openlumify.core.user.User;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Singleton
public class SimpleOrmSystemNotificationRepository extends SystemNotificationRepository {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(SimpleOrmSystemNotificationRepository.class);
    private SimpleOrmSession simpleOrmSession;
    private SimpleOrmContextProvider simpleOrmContextProvider;

    @Inject
    public SimpleOrmSystemNotificationRepository(
            Graph graph,
            GraphRepository graphRepository,
            GraphAuthorizationRepository graphAuthorizationRepository,
            UserRepository userRepository,
            SimpleOrmSession simpleOrmSession,
            SimpleOrmContextProvider simpleOrmContextProvider
    ) {
        super(graph, graphRepository, graphAuthorizationRepository, userRepository);
        this.simpleOrmSession = simpleOrmSession;
        this.simpleOrmContextProvider = simpleOrmContextProvider;
    }

    @Override
    public List<SystemNotification> getActiveNotifications(User user) {
        Date now = new Date();
        List<SystemNotification> activeNotifications = new ArrayList<>();
        for (SimpleOrmSystemNotification notification : simpleOrmSession.findAll(
                SimpleOrmSystemNotification.class,
                simpleOrmContextProvider.getContext(user)
        )) {
            if (notification.getStartDate().before(now)) {
                if (notification.getEndDate() == null || notification.getEndDate().after(now)) {
                    activeNotifications.add(SimpleOrmSystemNotification.toSystemNotification(notification));
                }
            }
        }
        LOGGER.debug("returning %d active system notifications", activeNotifications.size());
        return activeNotifications;
    }

    @Override
    public SystemNotification createNotification(SystemNotificationSeverity severity, String title, String message, String actionEvent, JSONObject actionPayload, Date startDate, Date endDate, User user) {
        if (startDate == null) {
            startDate = new Date();
        }
        SystemNotification notification = new SystemNotification(startDate, title, message, actionEvent, actionPayload);
        notification.setSeverity(severity);
        notification.setStartDate(startDate);
        notification.setEndDate(endDate);
        simpleOrmSession.save(new SimpleOrmSystemNotification(notification), VISIBILITY_STRING, simpleOrmContextProvider.getContext(user));
        return notification;
    }

    @Override
    public SystemNotification getNotification(String rowKey, User user) {
        return SimpleOrmSystemNotification.toSystemNotification(simpleOrmSession.findById(
                SimpleOrmSystemNotification.class,
                rowKey,
                simpleOrmContextProvider.getContext(user)
        ));
    }

    @Override
    public SystemNotification updateNotification(SystemNotification notification, User user) {
        simpleOrmSession.save(new SimpleOrmSystemNotification(notification), VISIBILITY_STRING, simpleOrmContextProvider.getContext(user));
        return notification;
    }

    @Override
    public List<SystemNotification> getFutureNotifications(Date maxDate, User user) {
        Date now = new Date();
        List<SystemNotification> futureNotifications = new ArrayList<>();
        for (SimpleOrmSystemNotification notification : simpleOrmSession.findAll(
                SimpleOrmSystemNotification.class,
                simpleOrmContextProvider.getContext(user)
        )) {
            if (notification.getStartDate().after(now) && (maxDate == null || notification.getStartDate().before(maxDate))) {
                futureNotifications.add(SimpleOrmSystemNotification.toSystemNotification(notification));
            }
        }
        LOGGER.debug("returning %d future system notifications", futureNotifications.size());
        return futureNotifications;
    }
}
