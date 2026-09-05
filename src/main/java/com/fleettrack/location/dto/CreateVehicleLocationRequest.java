package com.fleettrack.location.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateVehicleLocationRequest(

        @NotNull
        @DecimalMin("-90.000000")
        @DecimalMax("90.000000")
        @Digits(integer = 2, fraction = 6)
        BigDecimal latitude,

        @NotNull
        @DecimalMin("-180.000000")
        @DecimalMax("180.000000")
        @Digits(integer = 3, fraction = 6)
        BigDecimal longitude,

        @DecimalMin("0.00")
        @Digits(integer = 6, fraction = 2)
        BigDecimal speed,

        @DecimalMin("0.00")
        @DecimalMax("360.00")
        @Digits(integer = 3, fraction = 2)
        BigDecimal heading,

        @NotNull
        Instant recordedAt
) {
}