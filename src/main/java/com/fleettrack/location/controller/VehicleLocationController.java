package com.fleettrack.location.controller;

import com.fleettrack.location.dto.CreateVehicleLocationRequest;
import com.fleettrack.location.dto.VehicleLocationHistoryResponse;
import com.fleettrack.location.dto.VehicleLocationResponse;
import com.fleettrack.location.service.VehicleLocationService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;

@SecurityRequirement(name = "bearerAuth")
@Tag(
        name = "Vehicle Locations",
        description = "GPS ingestion, latest location and vehicle location history"
)
@RestController
@RequestMapping(
        "/api/v1/vehicles/{vehicleId}/locations"
)
public class VehicleLocationController {

    private final VehicleLocationService locationService;

    public VehicleLocationController(
            VehicleLocationService locationService
    ) {
        this.locationService =
                locationService;
    }

    @Operation(
            summary = "Record GPS location",
            description = """
                    Records a new GPS location for a vehicle.

                    After the database transaction commits, the same location
                    is broadcast in real time over the vehicle WebSocket topic.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Location recorded successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid coordinates or timestamp",
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
                    description = "Location cannot be recorded for this vehicle",
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
    public ResponseEntity<VehicleLocationResponse> record(
            @PathVariable
            Long vehicleId,

            @Valid
            @RequestBody
            CreateVehicleLocationRequest request
    ) {
        VehicleLocationResponse response =
                locationService.recordLocation(
                        vehicleId,
                        request
                );

        return ResponseEntity
                .created(
                        URI.create(
                                "/api/v1/vehicles/"
                                        + vehicleId
                                        + "/locations/"
                                        + response.id()
                        )
                )
                .body(response);
    }

    @Operation(
            summary = "Get latest vehicle location",
            description = """
                    Returns the most recently recorded GPS location.
                    This endpoint is backed by Redis caching.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Latest location returned successfully"
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
                    description = "Vehicle or location data not found",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(
                                    implementation = ProblemDetail.class
                            )
                    )
            )
    })
    @GetMapping("/latest")
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'FLEET_MANAGER')"
    )
    public ResponseEntity<VehicleLocationResponse> latest(
            @PathVariable
            Long vehicleId
    ) {
        return ResponseEntity.ok(
                locationService.getLatest(
                        vehicleId
                )
        );
    }

    @Operation(
            summary = "Get location history",
            description = """
                    Returns vehicle GPS history using cursor-based pagination.

                    Results are ordered by recordedAt descending
                    and id descending.

                    For the first request, omit cursorRecordedAt
                    and cursorId.

                    When hasNext is true, use nextCursor.recordedAt
                    and nextCursor.id in the following request.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Location history returned successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid cursor or page size",
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
    public ResponseEntity<VehicleLocationHistoryResponse> history(

            @PathVariable
            Long vehicleId,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            Instant cursorRecordedAt,

            @RequestParam(required = false)
            Long cursorId,

            @RequestParam(defaultValue = "20")
            int size
    ) {
        return ResponseEntity.ok(
                locationService.getHistory(
                        vehicleId,
                        cursorRecordedAt,
                        cursorId,
                        size
                )
        );
    }
}