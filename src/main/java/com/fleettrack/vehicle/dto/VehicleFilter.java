package com.fleettrack.vehicle.dto;

import com.fleettrack.vehicle.entity.VehicleStatus;

public record VehicleFilter(
        VehicleStatus status,
        String make,
        String model,
        Integer yearFrom,
        Integer yearTo,
        Long driverId
) {
}