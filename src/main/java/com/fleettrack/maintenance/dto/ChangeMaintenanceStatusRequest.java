package com.fleettrack.maintenance.dto;

import com.fleettrack.maintenance.entity.MaintenanceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ChangeMaintenanceStatusRequest(

        @NotNull
        MaintenanceStatus status,

        @NotNull
        @PositiveOrZero
        Long version
) {
}