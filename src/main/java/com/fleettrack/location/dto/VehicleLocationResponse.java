package com.fleettrack.location.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record VehicleLocationResponse(
        Long id,
        Long vehicleId,

        BigDecimal latitude,
        BigDecimal longitude,

        BigDecimal speed,
        BigDecimal heading,

        Instant recordedAt,
        Instant createdAt
) {
}