package org.openlumify.web.routes.notification;

import org.openlumify.webster.DefaultParameterValueConverter;
import org.openlumify.core.model.notification.SystemNotificationSeverity;

public class SystemNotificationSeverityValueConverter extends DefaultParameterValueConverter.SingleValueConverter<SystemNotificationSeverity> {
    @Override
    public SystemNotificationSeverity convert(Class parameterType, String parameterName, String value) {
        return SystemNotificationSeverity.valueOf(value);
    }
}
