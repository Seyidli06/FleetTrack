package com.fleettrack.location.dto;

import java.time.Instant;

public record VehicleLocationCursor(
        Instant recordedAt,
        Long id
) {
}