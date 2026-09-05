package com.fleettrack.maintenance.controller;

import com.fleettrack.common.dto.PageResponse;
import com.fleettrack.maintenance.dto.ChangeMaintenanceStatusRequest;
import com.fleettrack.maintenance.dto.CreateMaintenanceRequest;
import com.fleettrack.maintenance.dto.MaintenanceFilter;
import com.fleettrack.maintenance.dto.MaintenanceResponse;
import com.fleettrack.maintenance.dto.UpdateMaintenanceRequest;
import com.fleettrack.maintenance.entity.MaintenanceStatus;
import com.fleettrack.maintenance.service.MaintenanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;

@SecurityRequirement(name = "bearerAuth")
@Tag(
        name = "Maintenance",
        description = "Vehicle maintenance lifecycle, scheduling and service history"
)
@RestController
@RequestMapping("/api/v1")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    public MaintenanceController(
            MaintenanceService maintenanceService
    ) {
        this.maintenanceService =
                maintenanceService;
    }

    @Operation(
            summary = "Create maintenance",
            description = """
                    Creates a new SCHEDULED maintenance record
                    for a vehicle.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Maintenance created successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(
                                    implementation = ProblemDetail.class
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
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Maintenance cannot be created for this vehicle",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(
                                    implementation = ProblemDetail.class
                            )
                    )
            )
    })
    @PostMapping(
            "/vehicles/{vehicleId}/maintenance"
    )
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'FLEET_MANAGER')"
    )
    public ResponseEntity<MaintenanceResponse> create(
            @PathVariable
            Long vehicleId,

            @Valid
            @RequestBody
            CreateMaintenanceRequest request
    ) {
        MaintenanceResponse response =
                maintenanceService.create(
                        vehicleId,
                        request
                );

        return ResponseEntity
                .created(
                        URI.create(
                                "/api/v1/maintenance/"
                                        + response.id()
                        )
                )
                .body(response);
    }

    @Operation(
            summary = "Get maintenance",
            description = "Returns a maintenance record by identifier."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Maintenance returned successfully"
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
                    description = "Maintenance not found",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(
                                    implementation = ProblemDetail.class
                            )
                    )
            )
    })
    @GetMapping(
            "/maintenance/{id}"
    )
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'FLEET_MANAGER')"
    )
    public ResponseEntity<MaintenanceResponse> getById(
            @PathVariable
            Long id
    ) {
        return ResponseEntity.ok(
                maintenanceService.getById(id)
        );
    }

    @Operation(
            summary = "Get maintenance history",
            description = """
                    Returns paginated maintenance history for a vehicle
                    with optional status and date filters.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Maintenance history returned successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid filter or pagination parameters",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(
                                    implementation = ProblemDetail.class
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
            "/vehicles/{vehicleId}/maintenance"
    )
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'FLEET_MANAGER')"
    )
    public ResponseEntity<
            PageResponse<MaintenanceResponse>
            > getVehicleHistory(

            @PathVariable
            Long vehicleId,

            @RequestParam(required = false)
            MaintenanceStatus status,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate serviceDateFrom,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate serviceDateTo,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate nextServiceBefore,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size,

            @Parameter(
                    description = "Sort format: field,direction",
                    example = "serviceDate,desc"
            )
            @RequestParam(
                    defaultValue = "serviceDate,desc"
            )
            String sort
    ) {
        MaintenanceFilter filter =
                new MaintenanceFilter(
                        status,
                        serviceDateFrom,
                        serviceDateTo,
                        nextServiceBefore
                );

        return ResponseEntity.ok(
                maintenanceService
                        .getVehicleHistory(
                                vehicleId,
                                filter,
                                page,
                                size,
                                sort
                        )
        );
    }

    @Operation(
            summary = "Update maintenance",
            description = """
                    Updates editable maintenance information using
                    optimistic locking.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Maintenance updated successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(
                                    implementation = ProblemDetail.class
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
                    description = "Maintenance not found",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(
                                    implementation = ProblemDetail.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Terminal status or stale version conflict",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(
                                    implementation = ProblemDetail.class
                            )
                    )
            )
    })
    @PutMapping(
            "/maintenance/{id}"
    )
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'FLEET_MANAGER')"
    )
    public ResponseEntity<MaintenanceResponse> update(
            @PathVariable
            Long id,

            @Valid
            @RequestBody
            UpdateMaintenanceRequest request
    ) {
        return ResponseEntity.ok(
                maintenanceService.update(
                        id,
                        request
                )
        );
    }

    @Operation(
            summary = "Change maintenance status",
            description = """
                    Changes maintenance lifecycle status.

                    Allowed transitions:

                    SCHEDULED -> IN_PROGRESS or CANCELLED

                    IN_PROGRESS -> COMPLETED or CANCELLED
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Maintenance status changed successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(
                                    implementation = ProblemDetail.class
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
                    description = "Maintenance not found",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(
                                    implementation = ProblemDetail.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Illegal status transition or stale version",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(
                                    implementation = ProblemDetail.class
                            )
                    )
            )
    })
    @PatchMapping(
            "/maintenance/{id}/status"
    )
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'FLEET_MANAGER')"
    )
    public ResponseEntity<MaintenanceResponse> changeStatus(
            @PathVariable
            Long id,

            @Valid
            @RequestBody
            ChangeMaintenanceStatusRequest request
    ) {
        return ResponseEntity.ok(
                maintenanceService.changeStatus(
                        id,
                        request
                )
        );
    }

    @Operation(
            summary = "Delete scheduled maintenance",
            description = """
                    Deletes a maintenance record.
                    Only SCHEDULED maintenance may be deleted,
                    and ADMIN role is required.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Maintenance deleted successfully"
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
                    description = "ADMIN role required",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(
                                    implementation = ProblemDetail.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Maintenance not found",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(
                                    implementation = ProblemDetail.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Only SCHEDULED maintenance can be deleted",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(
                                    implementation = ProblemDetail.class
                            )
                    )
            )
    })
    @DeleteMapping(
            "/maintenance/{id}"
    )
    @PreAuthorize(
            "hasRole('ADMIN')"
    )
    public ResponseEntity<Void> delete(
            @PathVariable
            Long id
    ) {
        maintenanceService.delete(id);

        return ResponseEntity
                .noContent()
                .build();
    }
}