package org.openlumify.web.routes.notification;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.webster.ParameterizedHandler;
import org.openlumify.webster.annotations.Handle;
import org.openlumify.webster.annotations.Optional;
import org.apache.commons.lang.time.DateUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openlumify.core.model.notification.SystemNotification;
import org.openlumify.core.model.notification.SystemNotificationRepository;
import org.openlumify.core.model.notification.UserNotificationRepository;
import org.openlumify.core.user.User;

import java.util.Date;

@Singleton
public class Notifications implements ParameterizedHandler {
    private final SystemNotificationRepository systemNotificationRepository;
    private final UserNotificationRepository userNotificationRepository;

    @Inject
    public Notifications(
            final SystemNotificationRepository systemNotificationRepository,
            final UserNotificationRepository userNotificationRepository
    ) {
        this.systemNotificationRepository = systemNotificationRepository;
        this.userNotificationRepository = userNotificationRepository;
    }

    @Handle
    public JSONObject handle(
            @Optional(name = "futureDays", defaultValue = "10") int futureDays,
            User user
    ) throws Exception {
        JSONObject notifications = new JSONObject();

        JSONObject systemNotifications = new JSONObject();

        JSONArray activeNotifications = new JSONArray();
        for (SystemNotification notification : systemNotificationRepository.getActiveNotifications(user)) {
            activeNotifications.put(notification.toJSONObject());
        }
        systemNotifications.put("active", activeNotifications);

        Date maxDate = DateUtils.addDays(new Date(), futureDays);
        JSONArray futureNotifications = new JSONArray();
        for (SystemNotification notification : systemNotificationRepository.getFutureNotifications(maxDate, user)) {
            futureNotifications.put(notification.toJSONObject());
        }
        systemNotifications.put("future", futureNotifications);

        JSONArray userNotifications = new JSONArray();
        userNotificationRepository.getActiveNotifications(user)
                .map(notification -> notification.toJSONObject())
                .forEach(json -> userNotifications.put(json));

        notifications.put("system", systemNotifications);
        notifications.put("user", userNotifications);
        return notifications;
    }
}
