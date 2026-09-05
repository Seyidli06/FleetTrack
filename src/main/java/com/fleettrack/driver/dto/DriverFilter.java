package com.fleettrack.driver.dto;

import com.fleettrack.driver.entity.DriverStatus;

import java.time.LocalDate;

public record DriverFilter(
        DriverStatus status,
        String firstName,
        String lastName,
        String licenseCategory,
        LocalDate licenseExpiringAfter,
        LocalDate licenseExpiringBefore
) {
}