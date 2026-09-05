package com.fleettrack.driver.dto;

import com.fleettrack.driver.entity.DriverStatus;

import java.time.Instant;
import java.time.LocalDate;

public record DriverResponse(

        Long id,

        String firstName,

        String lastName,

        String licenseNumber,

        String licenseCategory,

        LocalDate licenseExpiryDate,

        String phone,

        String email,

        DriverStatus status,

        Instant createdAt,

        Instant updatedAt,

        Long version

) {
}