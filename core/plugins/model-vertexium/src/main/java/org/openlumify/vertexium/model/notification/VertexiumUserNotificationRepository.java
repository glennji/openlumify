package org.openlumify.vertexium.model.notification;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.json.JSONObject;
import org.vertexium.*;
import org.openlumify.core.model.graph.GraphRepository;
import org.openlumify.core.model.graph.GraphUpdateContext;
import org.openlumify.core.model.notification.ExpirationAge;
import org.openlumify.core.model.notification.NotificationOntology;
import org.openlumify.core.model.notification.UserNotification;
import org.openlumify.core.model.notification.UserNotificationRepository;
import org.openlumify.core.model.properties.types.PropertyMetadata;
import org.openlumify.core.model.user.GraphAuthorizationRepository;
import org.openlumify.core.model.workQueue.Priority;
import org.openlumify.core.model.workQueue.WorkQueueRepository;
import org.openlumify.core.security.OpenLumifyVisibility;
import org.openlumify.core.user.User;
import org.openlumify.vertexium.model.user.VertexiumUserRepository;
import org.openlumify.web.clientapi.model.VisibilityJson;

import java.util.ArrayList;
import java.util.Date;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.openlumify.core.util.StreamUtil.stream;

@Singleton
public class VertexiumUserNotificationRepository extends UserNotificationRepository {
    private WorkQueueRepository workQueueRepository;

    @Inject
    public VertexiumUserNotificationRepository(
            Graph graph,
            GraphRepository graphRepository,
            GraphAuthorizationRepository graphAuthorizationRepository,
            WorkQueueRepository workQueueRepository
    ) {
        super(graph, graphRepository, graphAuthorizationRepository);
        this.workQueueRepository = workQueueRepository;
    }

    public UserNotification createNotification(
            String userId,
            String title,
            String message,
            String actionEvent,
            JSONObject actionPayload,
            Date sentTime,
            ExpirationAge expirationAge,
            User authUser
    ) {
        UserNotification notification = new UserNotification(
                userId,
                title,
                message,
                actionEvent,
                actionPayload,
                sentTime,
                expirationAge
        );
        saveNotification(notification, authUser);
        return notification;
    }

    @Override
    protected Stream<UserNotification> findAll(User authUser) {
        return stream(getGraph().query(getAuthorizations(authUser))
                .hasExtendedData(NotificationOntology.USER_NOTIFICATIONS_TABLE)
                .extendedDataRows())
                .map(this::toUserNotification);
    }

    private UserNotification toUserNotification(ExtendedDataRow row) {
        if (row == null) {
            return null;
        }
        UserNotification notification = new UserNotification(
                row.getId().getRowId(),
                row.getId().getElementId(),
                NotificationOntology.USER_NOTIFICATIONS_TABLE_TITLE.getValue(row),
                NotificationOntology.USER_NOTIFICATIONS_TABLE_MESSAGE.getValue(row),
                NotificationOntology.USER_NOTIFICATIONS_TABLE_ACTION_EVENT.getValue(row),
                NotificationOntology.USER_NOTIFICATIONS_TABLE_ACTION_PAYLOAD.getValue(row),
                NotificationOntology.USER_NOTIFICATIONS_TABLE_SENT_DATE.getValue(row),
                NotificationOntology.USER_NOTIFICATIONS_TABLE_EXPIRATION_AGE.getValue(row)
        );
        notification.setMarkedRead(NotificationOntology.USER_NOTIFICATIONS_TABLE_MARKED_READ.getValue(row));
        notification.setNotified(NotificationOntology.USER_NOTIFICATIONS_TABLE_NOTIFIED.getValue(row));
        return notification;
    }

    @Override
    protected void saveNotification(UserNotification notification, User authUser) {
        Authorizations authorizations = getAuthorizations(authUser);
        try (GraphUpdateContext ctx = getGraphRepository().beginGraphUpdate(Priority.NORMAL, authUser, authorizations)) {
            Vertex userVertex = getUserVertex(notification.getUserId());
            ctx.update(userVertex, elemCtx -> {
                String row = notification.getId();
                VisibilityJson visibilityJson = new VisibilityJson();
                Visibility visibility = getVisibility();
                PropertyMetadata metadata = new PropertyMetadata(authUser, visibilityJson, visibility);

                NotificationOntology.USER_NOTIFICATIONS_TABLE_TITLE.addExtendedData(elemCtx, row, notification.getTitle(), metadata);
                if (notification.getActionEvent() != null && notification.getActionPayload() != null) {
                    NotificationOntology.USER_NOTIFICATIONS_TABLE_ACTION_EVENT.addExtendedData(elemCtx, row, notification.getActionEvent(), metadata);
                    NotificationOntology.USER_NOTIFICATIONS_TABLE_ACTION_PAYLOAD.addExtendedData(elemCtx, row, notification.getActionPayload(), metadata);
                }
                NotificationOntology.USER_NOTIFICATIONS_TABLE_MESSAGE.addExtendedData(elemCtx, row, notification.getMessage(), metadata);
                NotificationOntology.USER_NOTIFICATIONS_TABLE_MARKED_READ.addExtendedData(elemCtx, row, notification.isMarkedRead(), metadata);
                NotificationOntology.USER_NOTIFICATIONS_TABLE_NOTIFIED.addExtendedData(elemCtx, row, notification.isNotified(), metadata);
                NotificationOntology.USER_NOTIFICATIONS_TABLE_SENT_DATE.addExtendedData(elemCtx, row, notification.getSentDate(), metadata);
                if (notification.getExpirationAge() != null) {
                    NotificationOntology.USER_NOTIFICATIONS_TABLE_EXPIRATION_AGE.addExtendedData(elemCtx, row, notification.getExpirationAge(), metadata);
                }
            });
        }
        workQueueRepository.pushUserNotification(notification);
    }

    private Visibility getVisibility() {
        return new OpenLumifyVisibility(VISIBILITY_STRING).getVisibility();
    }

    @Override
    public UserNotification getNotification(String notificationId, User user) {
        Authorizations authorizations = getAuthorizations(user);
        Vertex userVertex = getUserVertex(user.getUserId());
        return toUserNotification(getGraph().getExtendedData(new ExtendedDataRowId(
                ElementType.VERTEX,
                userVertex.getId(),
                NotificationOntology.USER_NOTIFICATIONS_TABLE,
                notificationId
        ), authorizations));
    }

    @Override
    public void markRead(String[] notificationIds, User user) {
        for (String notificationId : notificationIds) {
            UserNotification notification = getNotification(notificationId, user);
            checkNotNull(notification, "Could not find notification with id " + notificationId);
            notification.setMarkedRead(true);
            saveNotification(notification, user);
        }
    }

    @Override
    public void markNotified(Iterable<String> notificationIds, User user) {
        for (String notificationId : notificationIds) {
            UserNotification notification = getNotification(notificationId, user);
            checkNotNull(notification, "Could not find notification with id " + notificationId);
            notification.setNotified(true);
            saveNotification(notification, user);
        }
    }

    private Vertex getUserVertex(String userId) {
        Vertex userVertex = ((VertexiumUserRepository) getUserRepository()).getUserVertex(userId);
        checkNotNull(userVertex, "Could not find user with id: " + userId);
        return userVertex;
    }
}
