package com.fleettrack.maintenance.reminder;

import com.fleettrack.maintenance.config.MaintenanceReminderProperties;
import com.fleettrack.maintenance.dto.MaintenanceReminderCandidate;
import com.fleettrack.maintenance.entity.MaintenanceStatus;
import com.fleettrack.maintenance.repository.MaintenanceRepository;
import com.fleettrack.vehicle.entity.VehicleStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Component
@ConditionalOnProperty(
        prefix = "app.maintenance-reminder",
        name = "enabled",
        havingValue = "true"
)
public class MaintenanceReminderScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(
                    MaintenanceReminderScheduler.class
            );

    private final MaintenanceRepository maintenanceRepository;
    private final MaintenanceReminderProcessor reminderProcessor;
    private final MaintenanceReminderProperties properties;

    public MaintenanceReminderScheduler(
            MaintenanceRepository maintenanceRepository,
            MaintenanceReminderProcessor reminderProcessor,
            MaintenanceReminderProperties properties
    ) {
        this.maintenanceRepository =
                maintenanceRepository;

        this.reminderProcessor =
                reminderProcessor;

        this.properties =
                properties;
    }

    @Scheduled(
            cron = "${app.maintenance-reminder.cron}",
            zone = "${app.maintenance-reminder.zone}"
    )
    @Transactional(readOnly = true)
    public void scanUpcomingMaintenance() {

        ZoneId zone =
                ZoneId.of(
                        properties.zone()
                );

        LocalDate today =
                LocalDate.now(zone);

        LocalDate threshold =
                today.plusDays(
                        properties.daysAhead()
                );

        log.info(
                "Scanning maintenance reminders from {} to {}",
                today,
                threshold
        );

        List<MaintenanceReminderCandidate> candidates =
                maintenanceRepository
                        .findReminderCandidates(
                                MaintenanceStatus.COMPLETED,
                                VehicleStatus.RETIRED,
                                today,
                                threshold
                        );

        log.info(
                "Found {} maintenance reminder candidate(s)",
                candidates.size()
        );

        for (
                MaintenanceReminderCandidate candidate
                : candidates
        ) {

            reminderProcessor.process(
                    candidate
            );
        }
    }
}