package org.openlumify.core.model.notification;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang.time.DateUtils;
import org.openlumify.core.config.Configuration;
import org.openlumify.core.model.lock.LockRepository;
import org.openlumify.core.model.user.UserRepository;
import org.openlumify.core.model.workQueue.WorkQueueRepository;
import org.openlumify.core.util.PeriodicBackgroundService;

import java.util.Date;

@Singleton
public class SystemNotificationService extends PeriodicBackgroundService {
    private static final Integer CHECK_INTERVAL_SECONDS_DEFAULT = 60;
    private static final String CHECK_INTERVAL_CONFIG_NAME = SystemNotificationService.class.getName() + ".checkIntervalSeconds";
    private final UserRepository userRepository;
    private final Integer checkIntervalSeconds;
    private final WorkQueueRepository workQueueRepository;
    private final SystemNotificationRepository systemNotificationRepository;

    @Inject
    public SystemNotificationService(
            Configuration configuration,
            UserRepository userRepository,
            LockRepository lockRepository,
            WorkQueueRepository workQueueRepository,
            SystemNotificationRepository systemNotificationRepository
    ) {
        super(lockRepository);
        this.userRepository = userRepository;
        this.checkIntervalSeconds = configuration.getInt(CHECK_INTERVAL_CONFIG_NAME, CHECK_INTERVAL_SECONDS_DEFAULT);
        this.workQueueRepository = workQueueRepository;
        this.systemNotificationRepository = systemNotificationRepository;
    }

    @Override
    protected void run() {
        Date now = new Date();
        Date nowPlusOneMinute = DateUtils.addSeconds(now, getCheckIntervalSeconds());
        systemNotificationRepository.getFutureNotifications(nowPlusOneMinute, userRepository.getSystemUser())
                .forEach(workQueueRepository::pushSystemNotification);
    }

    @Override
    protected int getCheckIntervalSeconds() {
        return checkIntervalSeconds;
    }
}
