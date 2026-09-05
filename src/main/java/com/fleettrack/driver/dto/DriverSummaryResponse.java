package com.fleettrack.driver.dto;

import com.fleettrack.driver.entity.DriverStatus;

import java.time.LocalDate;

public record DriverSummaryResponse(

        Long id,

        String firstName,

        String lastName,

        String licenseNumber,

        String licenseCategory,

        LocalDate licenseExpiryDate,

        DriverStatus status

) {
}
