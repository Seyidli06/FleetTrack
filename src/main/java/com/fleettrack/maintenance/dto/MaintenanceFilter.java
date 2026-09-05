package com.fleettrack.maintenance.dto;

import com.fleettrack.maintenance.entity.MaintenanceStatus;

import java.time.LocalDate;

public record MaintenanceFilter(
        MaintenanceStatus status,
        LocalDate serviceDateFrom,
        LocalDate serviceDateTo,
        LocalDate nextServiceBefore
) {
}