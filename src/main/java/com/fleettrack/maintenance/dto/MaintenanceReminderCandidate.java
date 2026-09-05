package com.fleettrack.maintenance.dto;

import java.time.LocalDate;

public record MaintenanceReminderCandidate(

        Long maintenanceId,

        Long vehicleId,

        String vin,

        String licensePlate,

        String maintenanceType,

        LocalDate nextServiceDate
) {
}