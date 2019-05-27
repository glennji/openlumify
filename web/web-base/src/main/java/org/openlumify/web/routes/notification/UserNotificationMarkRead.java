package org.openlumify.web.routes.notification;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.webster.ParameterizedHandler;
import org.openlumify.webster.annotations.Handle;
import org.openlumify.webster.annotations.Required;
import org.openlumify.core.exception.OpenLumifyAccessDeniedException;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.model.notification.UserNotification;
import org.openlumify.core.model.notification.UserNotificationRepository;
import org.openlumify.core.user.User;
import org.openlumify.web.OpenLumifyResponse;
import org.openlumify.web.clientapi.model.ClientApiSuccess;

@Singleton
public class UserNotificationMarkRead implements ParameterizedHandler {
    private final UserNotificationRepository userNotificationRepository;

    @Inject
    public UserNotificationMarkRead(final UserNotificationRepository userNotificationRepository) {
        this.userNotificationRepository = userNotificationRepository;
    }

    @Handle
    public ClientApiSuccess handle(
            @Required(name = "notificationIds[]") String[] notificationIds,
            User user
    ) throws Exception {
        for (String notificationId : notificationIds) {
            UserNotification notification = userNotificationRepository.getNotification(notificationId, user);
            if (notification == null) {
                throw new OpenLumifyResourceNotFoundException("Could not find notification with id: " + notificationId);
            }
            if (!notification.getUserId().equals(user.getUserId())) {
                throw new OpenLumifyAccessDeniedException(
                        "Cannot mark notification read that do not belong to you",
                        user,
                        notificationId
                );
            }
        }

        userNotificationRepository.markRead(notificationIds, user);
        return OpenLumifyResponse.SUCCESS;
    }
}
