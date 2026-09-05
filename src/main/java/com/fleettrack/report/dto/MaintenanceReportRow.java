package com.fleettrack.report.dto;

import com.fleettrack.maintenance.entity.MaintenanceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MaintenanceReportRow(

        Long maintenanceId,

        String maintenanceType,

        LocalDate serviceDate,

        LocalDate nextServiceDate,

        Long odometer,

        BigDecimal cost,

        MaintenanceStatus status
) {
}