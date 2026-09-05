package com.fleettrack.location.event;

import com.fleettrack.location.dto.VehicleLocationResponse;

public record VehicleLocationCreatedEvent(
        VehicleLocationResponse location
) {
}