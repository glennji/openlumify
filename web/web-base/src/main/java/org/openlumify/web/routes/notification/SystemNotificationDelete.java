package org.openlumify.web.routes.notification;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Required;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.model.notification.SystemNotification;
import org.openlumify.core.model.notification.SystemNotificationRepository;
import org.openlumify.core.model.workQueue.WorkQueueRepository;
import org.openlumify.core.user.User;
import org.openlumify.web.OpenLumifyResponse;
import org.openlumify.web.clientapi.model.ClientApiSuccess;

@Singleton
public class SystemNotificationDelete implements ParameterizedHandler {
    private final SystemNotificationRepository systemNotificationRepository;
    private final WorkQueueRepository workQueueRepository;

    @Inject
    public SystemNotificationDelete(
            final SystemNotificationRepository systemNotificationRepository,
            final WorkQueueRepository workQueueRepository
    ) {
        this.systemNotificationRepository = systemNotificationRepository;
        this.workQueueRepository = workQueueRepository;
    }

    @Handle
    public ClientApiSuccess handle(
            @Required(name = "notificationId") String notificationId,
            User user
    ) throws Exception {
        SystemNotification notification = systemNotificationRepository.getNotification(notificationId, user);
        if (notification == null) {
            throw new OpenLumifyResourceNotFoundException("Could not find notification with id: " + notificationId);
        }

        systemNotificationRepository.endNotification(notification, user);
        workQueueRepository.pushSystemNotificationEnded(notificationId);
        return OpenLumifyResponse.SUCCESS;
    }
}
