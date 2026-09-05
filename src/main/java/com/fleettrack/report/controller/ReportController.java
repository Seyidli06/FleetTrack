package com.fleettrack.report.controller;

import com.fleettrack.report.dto.PdfReport;
import com.fleettrack.report.service.VehicleMaintenanceReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(
        name = "bearerAuth"
)
@Tag(
        name = "Reports",
        description = "FleetTrack PDF report generation"
)
@RestController
@RequestMapping(
        "/api/v1/reports"
)
public class ReportController {

    private final VehicleMaintenanceReportService
            reportService;

    public ReportController(
            VehicleMaintenanceReportService reportService
    ) {
        this.reportService =
                reportService;
    }

    @Operation(
            summary = "Generate vehicle maintenance PDF",
            description = """
                    Generates and downloads a PDF containing vehicle
                    information and complete maintenance history.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "PDF report generated successfully",
                    content = @Content(
                            mediaType = "application/pdf",
                            schema = @Schema(
                                    type = "string",
                                    format = "binary"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(
                                    implementation = ProblemDetail.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Insufficient permissions",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(
                                    implementation = ProblemDetail.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Vehicle not found",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(
                                    implementation = ProblemDetail.class
                            )
                    )
            )
    })
    @GetMapping(
            value = "/vehicles/{vehicleId}/maintenance.pdf",
            produces = MediaType.APPLICATION_PDF_VALUE
    )
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'FLEET_MANAGER')"
    )
    public ResponseEntity<byte[]> generateMaintenanceReport(
            @PathVariable
            Long vehicleId
    ) {
        PdfReport report =
                reportService.generate(
                        vehicleId
                );

        ContentDisposition disposition =
                ContentDisposition
                        .attachment()
                        .filename(
                                report.fileName()
                        )
                        .build();

        return ResponseEntity
                .ok()
                .contentType(
                        MediaType.APPLICATION_PDF
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition.toString()
                )
                .contentLength(
                        report.content().length
                )
                .body(
                        report.content()
                );
    }
}