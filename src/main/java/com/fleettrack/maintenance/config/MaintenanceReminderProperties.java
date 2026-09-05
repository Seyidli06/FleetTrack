package com.fleettrack.maintenance.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(
        prefix = "app.maintenance-reminder"
)
@Validated
public record MaintenanceReminderProperties(

        boolean enabled,

        @Min(1)
        @Max(30)
        int daysAhead,

        @NotBlank
        String cron,

        @NotBlank
        String zone
) {
}