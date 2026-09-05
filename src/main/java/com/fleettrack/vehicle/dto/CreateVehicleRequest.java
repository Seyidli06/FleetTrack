package com.fleettrack.vehicle.dto;

import com.fleettrack.vehicle.entity.VehicleStatus;
import jakarta.validation.constraints.*;

public record CreateVehicleRequest(

        @NotBlank
        @Pattern(
                regexp = "^[A-HJ-NPR-Z0-9]{17}$",
                message = "VIN must contain exactly 17 valid characters"
        )
        String vin,

        @NotBlank
        @Size(max = 20)
        String licensePlate,

        @NotBlank
        @Size(max = 100)
        String make,

        @NotBlank
        @Size(max = 100)
        String model,

        @NotNull
        @Min(1886)
        Integer manufactureYear,

        @NotNull
        VehicleStatus status

) {
}