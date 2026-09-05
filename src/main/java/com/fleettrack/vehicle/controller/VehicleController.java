package com.fleettrack.vehicle.controller;

import com.fleettrack.common.dto.PageResponse;
import com.fleettrack.vehicle.dto.CreateVehicleRequest;
import com.fleettrack.vehicle.dto.UpdateVehicleRequest;
import com.fleettrack.vehicle.dto.VehicleFilter;
import com.fleettrack.vehicle.dto.VehicleResponse;
import com.fleettrack.vehicle.dto.VehicleSummaryResponse;
import com.fleettrack.vehicle.entity.VehicleStatus;
import com.fleettrack.vehicle.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@SecurityRequirement(name = "bearerAuth")
@Tag(
        name = "Vehicles",
        description = "Vehicle registration, retrieval, filtering and lifecycle management"
)
@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(
            VehicleService vehicleService
    ) {
        this.vehicleService =
                vehicleService;
    }

    @Operation(
            summary = "Create vehicle",
            description = "Registers a new vehicle in the fleet."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Vehicle created successfully"
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
                    responseCode = "409",
                    description = "VIN or license plate already exists",
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
    public ResponseEntity<VehicleResponse> create(
            @Valid
            @RequestBody
            CreateVehicleRequest request
    ) {
        VehicleResponse response =
                vehicleService.create(request);

        return ResponseEntity
                .created(
                        URI.create(
                                "/api/v1/vehicles/"
                                        + response.id()
                        )
                )
                .body(response);
    }

    @Operation(
            summary = "Get vehicle",
            description = "Returns a vehicle by its identifier."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Vehicle returned successfully"
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
    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'FLEET_MANAGER')"
    )
    public ResponseEntity<VehicleResponse> getById(
            @Parameter(
                    description = "Vehicle identifier",
                    example = "1"
            )
            @PathVariable
            Long id
    ) {
        return ResponseEntity.ok(
                vehicleService.getById(id)
        );
    }

    @Operation(
            summary = "Search vehicles",
            description = """
                    Returns a paginated vehicle list with optional dynamic
                    filtering and whitelisted sorting.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Vehicles returned successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid filter, pagination or sort parameter",
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
            )
    })
    @GetMapping
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'FLEET_MANAGER')"
    )
    public ResponseEntity<
            PageResponse<VehicleSummaryResponse>
            > getAll(

            @RequestParam(required = false)
            VehicleStatus status,

            @RequestParam(required = false)
            String make,

            @RequestParam(required = false)
            String model,

            @RequestParam(required = false)
            Integer yearFrom,

            @RequestParam(required = false)
            Integer yearTo,

            @RequestParam(required = false)
            Long driverId,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size,

            @Parameter(
                    description = "Sort format: field,direction",
                    example = "manufactureYear,desc"
            )
            @RequestParam(
                    defaultValue = "createdAt,desc"
            )
            String sort
    ) {
        VehicleFilter filter =
                new VehicleFilter(
                        status,
                        make,
                        model,
                        yearFrom,
                        yearTo,
                        driverId
                );

        return ResponseEntity.ok(
                vehicleService.getAll(
                        filter,
                        page,
                        size,
                        sort
                )
        );
    }

    @Operation(
            summary = "Update vehicle",
            description = """
                    Updates vehicle information using optimistic locking.
                    The request version must match the current resource version.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Vehicle updated successfully"
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
                    description = "Duplicate data or stale version",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(
                                    implementation = ProblemDetail.class
                            )
                    )
            )
    })
    @PutMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'FLEET_MANAGER')"
    )
    public ResponseEntity<VehicleResponse> update(
            @PathVariable
            Long id,

            @Valid
            @RequestBody
            UpdateVehicleRequest request
    ) {
        return ResponseEntity.ok(
                vehicleService.update(
                        id,
                        request
                )
        );
    }

    @Operation(
            summary = "Delete vehicle",
            description = """
                    Deletes a vehicle. This operation is restricted to ADMIN users.
                    Database foreign-key constraints may prevent deletion when
                    historical records reference the vehicle.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Vehicle deleted successfully"
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
                    description = "Vehicle is referenced by another resource",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(
                                    implementation = ProblemDetail.class
                            )
                    )
            )
    })
    @DeleteMapping("/{id}")
    @PreAuthorize(
            "hasRole('ADMIN')"
    )
    public ResponseEntity<Void> delete(
            @PathVariable
            Long id
    ) {
        vehicleService.delete(id);

        return ResponseEntity
                .noContent()
                .build();
    }
}