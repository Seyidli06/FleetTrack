package com.fleettrack.maintenance.dto;

import com.fleettrack.maintenance.entity.MaintenanceStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record MaintenanceResponse(
        Long id,

        Long vehicleId,
        String vin,
        String licensePlate,

        String maintenanceType,
        String description,

        LocalDate serviceDate,
        LocalDate nextServiceDate,

        Long odometer,
        BigDecimal cost,

        MaintenanceStatus status,

        Instant createdAt,
        Instant updatedAt,

        Long version
) {
}