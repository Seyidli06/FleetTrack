package com.fleettrack.assignment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AssignVehicleRequest(

        @NotNull
        @Positive
        Long driverId

) {
}