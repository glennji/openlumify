package org.openlumify.core.process;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.core.model.notification.SystemNotificationService;

@Singleton
public class SystemNotificationProcess implements OpenLumifyProcess {
    private final SystemNotificationService systemNotificationService;

    @Inject
    public SystemNotificationProcess(SystemNotificationService systemNotificationService) {
        this.systemNotificationService = systemNotificationService;
    }

    @Override
    public void startProcess(OpenLumifyProcessOptions options) {
        this.systemNotificationService.start();
    }
}
