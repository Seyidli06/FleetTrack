package com.fleettrack.maintenance.reminder;

import com.fleettrack.config.async.AsyncConfig;
import com.fleettrack.maintenance.config.MaintenanceReminderProperties;
import com.fleettrack.maintenance.dto.MaintenanceReminderCandidate;
import com.fleettrack.notification.dto.NotificationEvent;
import com.fleettrack.notification.model.NotificationType;
import com.fleettrack.notification.publisher.RedisNotificationPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Component
public class MaintenanceReminderProcessor {

    private static final Logger log =
            LoggerFactory.getLogger(
                    MaintenanceReminderProcessor.class
            );

    private final MaintenanceReminderProperties properties;
    private final RedisNotificationPublisher notificationPublisher;

    public MaintenanceReminderProcessor(
            MaintenanceReminderProperties properties,
            RedisNotificationPublisher notificationPublisher
    ) {
        this.properties =
                properties;

        this.notificationPublisher =
                notificationPublisher;
    }

    @Async(
            AsyncConfig.MAINTENANCE_REMINDER_EXECUTOR
    )
    public void process(
            MaintenanceReminderCandidate candidate
    ) {
        try {

            ZoneId zone =
                    ZoneId.of(
                            properties.zone()
                    );

            LocalDate today =
                    LocalDate.now(zone);

            long daysUntilDue =
                    ChronoUnit.DAYS.between(
                            today,
                            candidate.nextServiceDate()
                    );

            NotificationEvent event =
                    new NotificationEvent(
                            UUID.randomUUID().toString(),
                            NotificationType.MAINTENANCE_DUE_SOON,
                            candidate.vehicleId(),
                            candidate.maintenanceId(),
                            "Maintenance due soon",
                            createMessage(
                                    candidate,
                                    daysUntilDue
                            ),
                            Instant.now()
                    );

            notificationPublisher.publish(
                    event
            );

            log.info(
                    "Maintenance reminder published, maintenanceId={}, vehicleId={}",
                    candidate.maintenanceId(),
                    candidate.vehicleId()
            );

        } catch (Exception exception) {

            log.error(
                    "Failed to process maintenance reminder, maintenanceId={}",
                    candidate.maintenanceId(),
                    exception
            );
        }
    }

    private String createMessage(
            MaintenanceReminderCandidate candidate,
            long daysUntilDue
    ) {
        return "Vehicle "
                + candidate.licensePlate()
                + " requires "
                + candidate.maintenanceType()
                + " maintenance in "
                + daysUntilDue
                + " day(s). Due date: "
                + candidate.nextServiceDate();
    }
}