package org.openlumify.core.model.notification;

import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.model.properties.types.*;

public class NotificationOntology {
    /*************************************
     * BEGIN GENERATED CODE
     *************************************/

    public static final String IRI = "http://openlumify.org/notification";

    public static final String SYSTEM_NOTIFICATIONS_TABLE = "http://openlumify.org/notification#systemNotificationsTable";
    public static final StringOpenLumifyExtendedData SYSTEM_NOTIFICATIONS_TABLE_ACTION_EVENT = new StringOpenLumifyExtendedData("http://openlumify.org/notification#systemNotificationsTable", "http://openlumify.org/notification#actionEvent");
    public static final DateOpenLumifyExtendedData SYSTEM_NOTIFICATIONS_TABLE_END_DATE = new DateOpenLumifyExtendedData("http://openlumify.org/notification#systemNotificationsTable", "http://openlumify.org/notification#endDate");
    public static final StringOpenLumifyExtendedData SYSTEM_NOTIFICATIONS_TABLE_MESSAGE = new StringOpenLumifyExtendedData("http://openlumify.org/notification#systemNotificationsTable", "http://openlumify.org/notification#message");
    public static final DateOpenLumifyExtendedData SYSTEM_NOTIFICATIONS_TABLE_START_DATE = new DateOpenLumifyExtendedData("http://openlumify.org/notification#systemNotificationsTable", "http://openlumify.org/notification#startDate");
    public static final StringOpenLumifyExtendedData SYSTEM_NOTIFICATIONS_TABLE_TITLE = new StringOpenLumifyExtendedData("http://openlumify.org/notification#systemNotificationsTable", "http://openlumify.org/notification#title");
    public static final String USER_NOTIFICATIONS_TABLE = "http://openlumify.org/notification#userNotificationsTable";
    public static final StringOpenLumifyExtendedData USER_NOTIFICATIONS_TABLE_ACTION_EVENT = new StringOpenLumifyExtendedData("http://openlumify.org/notification#userNotificationsTable", "http://openlumify.org/notification#actionEvent");
    public static final BooleanOpenLumifyExtendedData USER_NOTIFICATIONS_TABLE_MARKED_READ = new BooleanOpenLumifyExtendedData("http://openlumify.org/notification#userNotificationsTable", "http://openlumify.org/notification#markedRead");
    public static final StringOpenLumifyExtendedData USER_NOTIFICATIONS_TABLE_MESSAGE = new StringOpenLumifyExtendedData("http://openlumify.org/notification#userNotificationsTable", "http://openlumify.org/notification#message");
    public static final BooleanOpenLumifyExtendedData USER_NOTIFICATIONS_TABLE_NOTIFIED = new BooleanOpenLumifyExtendedData("http://openlumify.org/notification#userNotificationsTable", "http://openlumify.org/notification#notified");
    public static final DateOpenLumifyExtendedData USER_NOTIFICATIONS_TABLE_SENT_DATE = new DateOpenLumifyExtendedData("http://openlumify.org/notification#userNotificationsTable", "http://openlumify.org/notification#sentDate");
    public static final StringOpenLumifyExtendedData USER_NOTIFICATIONS_TABLE_TITLE = new StringOpenLumifyExtendedData("http://openlumify.org/notification#userNotificationsTable", "http://openlumify.org/notification#title");

    /*************************************
     * END GENERATED CODE
     *************************************/

    public static final JsonOpenLumifyExtendedData SYSTEM_NOTIFICATIONS_TABLE_ACTION_PAYLOAD = new JsonOpenLumifyExtendedData("http://openlumify.org/notification#systemNotificationsTable", "http://openlumify.org/notification#actionPayload");
    public static final JsonOpenLumifyExtendedData USER_NOTIFICATIONS_TABLE_ACTION_PAYLOAD = new JsonOpenLumifyExtendedData("http://openlumify.org/notification#userNotificationsTable", "http://openlumify.org/notification#actionPayload");
    public static final SystemNotificationSeverityOpenLumifyExtendedData SYSTEM_NOTIFICATIONS_TABLE_SEVERITY = new SystemNotificationSeverityOpenLumifyExtendedData("http://openlumify.org/notification#systemNotificationsTable", "http://openlumify.org/notification#severity");
    public static final ExpirationAgeOpenLumifyExtendedData USER_NOTIFICATIONS_TABLE_EXPIRATION_AGE = new ExpirationAgeOpenLumifyExtendedData("http://openlumify.org/notification#userNotificationsTable", "http://openlumify.org/notification#expirationAge");

    public static class SystemNotificationSeverityOpenLumifyExtendedData extends OpenLumifyExtendedData<SystemNotificationSeverity, String> {
        public SystemNotificationSeverityOpenLumifyExtendedData(String tableName, String columnName) {
            super(tableName, columnName);
        }

        @Override
        public String rawToGraph(SystemNotificationSeverity value) {
            if (value == null) {
                return null;
            }
            return value.name();
        }

        @Override
        public SystemNotificationSeverity graphToRaw(Object value) {
            if (value == null) {
                return null;
            }
            if (value instanceof String) {
                return SystemNotificationSeverity.valueOf((String) value);
            }
            throw new OpenLumifyException("Unhandled value: " + value + " (type: " + value.getClass().getName() + ")");
        }
    }

    public static class ExpirationAgeOpenLumifyExtendedData extends OpenLumifyExtendedData<ExpirationAge, String> {
        public ExpirationAgeOpenLumifyExtendedData(String tableName, String columnName) {
            super(tableName, columnName);
        }

        @Override
        public String rawToGraph(ExpirationAge value) {
            if (value == null) {
                return null;
            }
            return value.toString();
        }

        @Override
        public ExpirationAge graphToRaw(Object value) {
            if (value == null) {
                return null;
            }
            if (value instanceof String) {
                return ExpirationAge.parse((String) value);
            }
            throw new OpenLumifyException("Unhandled value: " + value + " (type: " + value.getClass().getName() + ")");
        }
    }
}
