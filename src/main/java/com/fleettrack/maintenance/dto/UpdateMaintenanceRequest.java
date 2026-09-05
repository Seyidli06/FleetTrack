package com.fleettrack.maintenance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateMaintenanceRequest(

        @NotBlank
        @Size(max = 50)
        String maintenanceType,

        @Size(max = 1000)
        String description,

        @NotNull
        LocalDate serviceDate,

        LocalDate nextServiceDate,

        @PositiveOrZero
        Long odometer,

        @DecimalMin(value = "0.00")
        @Digits(integer = 10, fraction = 2)
        BigDecimal cost,

        @NotNull
        @PositiveOrZero
        Long version
) {
}