package com.fleettrack.config.scheduling;

import com.fleettrack.maintenance.config.MaintenanceReminderProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(
        MaintenanceReminderProperties.class
)
public class SchedulingConfig {
}