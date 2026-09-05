package com.fleettrack.driver.dto;

import com.fleettrack.driver.entity.DriverStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateDriverRequest(

        @NotBlank
        @Size(max = 100)
        String firstName,

        @NotBlank
        @Size(max = 100)
        String lastName,

        @NotBlank
        @Size(max = 50)
        String licenseNumber,

        @NotBlank
        @Size(max = 20)
        String licenseCategory,

        @NotNull
        LocalDate licenseExpiryDate,

        @Size(max = 30)
        String phone,

        @Email
        @Size(max = 255)
        String email,

        @NotNull
        DriverStatus status

) {
}