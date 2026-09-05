package com.fleettrack.vehicle.dto;

import com.fleettrack.vehicle.entity.VehicleStatus;

public record VehicleSummaryResponse(

        Long id,

        String vin,

        String licensePlate,

        String make,

        String model,

        Integer manufactureYear,

        VehicleStatus status

) {
}