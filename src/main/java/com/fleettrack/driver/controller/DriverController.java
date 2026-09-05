package com.fleettrack.driver.controller;

import com.fleettrack.common.dto.PageResponse;
import com.fleettrack.driver.dto.CreateDriverRequest;
import com.fleettrack.driver.dto.DriverFilter;
import com.fleettrack.driver.dto.DriverResponse;
import com.fleettrack.driver.dto.DriverSummaryResponse;
import com.fleettrack.driver.dto.UpdateDriverRequest;
import com.fleettrack.driver.entity.DriverStatus;
import com.fleettrack.driver.service.DriverService;
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
        name = "Drivers",
        description = "Driver profile, license and status management"
)
@RestController
@RequestMapping("/api/v1/drivers")
public class DriverController {

    private final DriverService driverService;

    public DriverController(
            DriverService driverService
    ) {
        this.driverService =
                driverService;
    }

    @Operation(
            summary = "Create driver",
            description = "Creates a new driver profile."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Driver created successfully"
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
                    description = "License number already exists",
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
    public ResponseEntity<DriverResponse> create(
            @Valid
            @RequestBody
            CreateDriverRequest request
    ) {
        DriverResponse response =
                driverService.create(request);

        return ResponseEntity
                .created(
                        URI.create(
                                "/api/v1/drivers/"
                                        + response.id()
                        )
                )
                .body(response);
    }

    @Operation(
            summary = "Get driver",
            description = "Returns a driver by identifier."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Driver returned successfully"
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
                    description = "Driver not found",
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
    public ResponseEntity<DriverResponse> getById(
            @PathVariable
            Long id
    ) {
        return ResponseEntity.ok(
                driverService.getById(id)
        );
    }

    @Operation(
            summary = "Search drivers",
            description = """
                    Returns a paginated driver list with dynamic filters
                    and whitelisted sorting.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Drivers returned successfully"
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
            PageResponse<DriverSummaryResponse>
            > getAll(

            @RequestParam(required = false)
            DriverStatus status,

            @RequestParam(required = false)
            String firstName,

            @RequestParam(required = false)
            String lastName,

            @RequestParam(required = false)
            String licenseCategory,

            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            @RequestParam(required = false)
            LocalDate licenseExpiringAfter,

            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            @RequestParam(required = false)
            LocalDate licenseExpiringBefore,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size,

            @Parameter(
                    description = "Sort format: field,direction",
                    example = "licenseExpiryDate,asc"
            )
            @RequestParam(
                    defaultValue = "createdAt,desc"
            )
            String sort
    ) {
        DriverFilter filter =
                new DriverFilter(
                        status,
                        firstName,
                        lastName,
                        licenseCategory,
                        licenseExpiringAfter,
                        licenseExpiringBefore
                );

        return ResponseEntity.ok(
                driverService.getAll(
                        filter,
                        page,
                        size,
                        sort
                )
        );
    }

    @Operation(
            summary = "Update driver",
            description = """
                    Updates a driver using optimistic locking.
                    The request version must match the current resource version.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Driver updated successfully"
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
                    description = "Driver not found",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(
                                    implementation = ProblemDetail.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Duplicate license or stale version",
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
    public ResponseEntity<DriverResponse> update(
            @PathVariable
            Long id,

            @Valid
            @RequestBody
            UpdateDriverRequest request
    ) {
        return ResponseEntity.ok(
                driverService.update(
                        id,
                        request
                )
        );
    }

    @Operation(
            summary = "Delete driver",
            description = "Deletes a driver. ADMIN role is required."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Driver deleted successfully"
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
                    description = "Driver not found",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(
                                    implementation = ProblemDetail.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Driver is referenced by another resource",
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
        driverService.delete(id);

        return ResponseEntity
                .noContent()
                .build();
    }
}