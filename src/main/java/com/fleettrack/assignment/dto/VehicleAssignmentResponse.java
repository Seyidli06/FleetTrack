package com.fleettrack.assignment.dto;

import java.time.Instant;

public record VehicleAssignmentResponse(

        Long id,

        Long vehicleId,

        String vin,

        String licensePlate,

        Long driverId,

        String driverName,

        Instant assignedAt,

        Instant unassignedAt,

        boolean active

) {
}