package com.fleettrack.vehicle.dto;

import com.fleettrack.vehicle.entity.VehicleStatus;

import java.time.Instant;

public record VehicleResponse(

        Long id,

        String vin,

        String licensePlate,

        String make,

        String model,

        Integer manufactureYear,

        VehicleStatus status,

        Instant createdAt,

        Instant updatedAt,

        Long version

) {
}