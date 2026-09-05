package com.fleettrack.report.service;

import com.fleettrack.maintenance.repository.MaintenanceRepository;
import com.fleettrack.report.dto.MaintenanceReportRow;
import com.fleettrack.report.dto.PdfReport;
import com.fleettrack.report.pdf.VehicleMaintenancePdfGenerator;
import com.fleettrack.vehicle.dto.VehicleResponse;
import com.fleettrack.vehicle.service.VehicleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VehicleMaintenanceReportService {

    private final VehicleService vehicleService;
    private final MaintenanceRepository maintenanceRepository;
    private final VehicleMaintenancePdfGenerator pdfGenerator;

    public VehicleMaintenanceReportService(
            VehicleService vehicleService,
            MaintenanceRepository maintenanceRepository,
            VehicleMaintenancePdfGenerator pdfGenerator
    ) {
        this.vehicleService = vehicleService;
        this.maintenanceRepository = maintenanceRepository;
        this.pdfGenerator = pdfGenerator;
    }

    @Transactional(readOnly = true)
    public PdfReport generate(Long vehicleId) {

        VehicleResponse vehicle =
                vehicleService.getById(vehicleId);

        List<MaintenanceReportRow> rows =
                maintenanceRepository
                        .findReportRowsByVehicleId(vehicleId);

        byte[] content =
                pdfGenerator.generate(
                        vehicle,
                        rows
                );

        String fileName =
                "vehicle-"
                        + vehicleId
                        + "-maintenance-report.pdf";

        return new PdfReport(
                fileName,
                content
        );
    }
}