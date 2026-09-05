package com.fleettrack.assignment.controller;

import com.fleettrack.assignment.dto.AssignVehicleRequest;
import com.fleettrack.assignment.dto.VehicleAssignmentResponse;
import com.fleettrack.assignment.service.VehicleAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@SecurityRequirement(name = "bearerAuth")
@Tag(
        name = "Vehicle Assignments",
        description = "Driver-to-vehicle assignment lifecycle and history"
)
@RestController
@RequestMapping(
        "/api/v1/vehicles/{vehicleId}/assignments"
)
public class VehicleAssignmentController {

    private final VehicleAssignmentService assignmentService;

    public VehicleAssignmentController(
            VehicleAssignmentService assignmentService
    ) {
        this.assignmentService =
                assignmentService;
    }

    @Operation(
            summary = "Assign driver to vehicle",
            description = """
                    Creates an active driver-to-vehicle assignment.

                    The vehicle must be ACTIVE and the driver must be ACTIVE
                    with a non-expired driving license.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Assignment created successfully"
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
                    description = "Vehicle or driver not found",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(
                                    implementation = ProblemDetail.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Assignment business rule conflict",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(
                                    implementation = ProblemDetail.class
                            )
                    )
            )
    })
    @PostMapping
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'FLEET_MANAGER')"
    )
    public ResponseEntity<VehicleAssignmentResponse> assign(
            @PathVariable
            Long vehicleId,

            @Valid
            @RequestBody
            AssignVehicleRequest request
    ) {
        VehicleAssignmentResponse response =
                assignmentService.assign(
                        vehicleId,
                        request
                );

        return ResponseEntity
                .created(
                        URI.create(
                                "/api/v1/vehicles/"
                                        + vehicleId
                                        + "/assignments/"
                                        + response.id()
                        )
                )
                .body(response);
    }

    @Operation(
            summary = "Get current assignment",
            description = "Returns the currently active assignment for a vehicle."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Current assignment returned successfully"
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
                    description = "Vehicle or active assignment not found",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(
                                    implementation = ProblemDetail.class
                            )
                    )
            )
    })
    @GetMapping("/current")
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'FLEET_MANAGER')"
    )
    public ResponseEntity<VehicleAssignmentResponse> getCurrent(
            @PathVariable
            Long vehicleId
    ) {
        return ResponseEntity.ok(
                assignmentService.getCurrent(
                        vehicleId
                )
        );
    }

    @Operation(
            summary = "Get assignment history",
            description = """
                    Returns the complete assignment history for a vehicle,
                    including inactive assignments.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Assignment history returned successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid pagination parameters",
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
    @GetMapping
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'FLEET_MANAGER')"
    )
    public ResponseEntity<
            Page<VehicleAssignmentResponse>
            > getHistory(

            @PathVariable
            Long vehicleId,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {
        return ResponseEntity.ok(
                assignmentService.getHistory(
                        vehicleId,
                        page,
                        size
                )
        );
    }

    @Operation(
            summary = "Unassign current driver",
            description = """
                    Closes the active assignment by setting unassignedAt.
                    Historical assignment data is preserved.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Vehicle unassigned successfully"
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
                    description = "Vehicle or active assignment not found",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(
                                    implementation = ProblemDetail.class
                            )
                    )
            )
    })
    @DeleteMapping("/current")
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'FLEET_MANAGER')"
    )
    public ResponseEntity<VehicleAssignmentResponse> unassign(
            @PathVariable
            Long vehicleId
    ) {
        return ResponseEntity.ok(
                assignmentService.unassign(
                        vehicleId
                )
        );
    }
}