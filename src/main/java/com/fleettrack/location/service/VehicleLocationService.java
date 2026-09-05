package com.fleettrack.location.service;

import com.fleettrack.common.cache.CacheNames;
import com.fleettrack.common.dto.PageResponse;
import com.fleettrack.common.exception.BusinessRuleViolationException;
import com.fleettrack.common.exception.ResourceNotFoundException;
import com.fleettrack.location.dto.CreateVehicleLocationRequest;
import com.fleettrack.location.dto.VehicleLocationResponse;
import com.fleettrack.location.entity.VehicleLocation;
import com.fleettrack.location.event.VehicleLocationCreatedEvent;
import com.fleettrack.location.repository.VehicleLocationRepository;
import com.fleettrack.vehicle.entity.Vehicle;
import com.fleettrack.vehicle.entity.VehicleStatus;
import com.fleettrack.vehicle.repository.VehicleRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fleettrack.location.dto.VehicleLocationCursor;
import com.fleettrack.location.dto.VehicleLocationHistoryResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import java.util.List;

import java.time.Instant;

@Service
public class VehicleLocationService {

    private static final int MAX_PAGE_SIZE = 100;

    private static final long MAX_FUTURE_CLOCK_SKEW_SECONDS =
            300;

    private final VehicleLocationRepository locationRepository;
    private final VehicleRepository vehicleRepository;
    private final ApplicationEventPublisher eventPublisher;

    public VehicleLocationService(
            VehicleLocationRepository locationRepository,
            VehicleRepository vehicleRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.locationRepository =
                locationRepository;

        this.vehicleRepository =
                vehicleRepository;

        this.eventPublisher =
                eventPublisher;
    }

    @CacheEvict(
            cacheNames = CacheNames.LATEST_VEHICLE_LOCATION,
            key = "#vehicleId"
    )
    @Transactional
    public VehicleLocationResponse recordLocation(
            Long vehicleId,
            CreateVehicleLocationRequest request
    ) {
        Vehicle vehicle =
                getVehicle(vehicleId);

        validateVehicle(
                vehicle
        );

        validateRecordedAt(
                request.recordedAt()
        );

        VehicleLocation location =
                new VehicleLocation();

        location.setVehicle(
                vehicle
        );

        location.setLatitude(
                request.latitude()
        );

        location.setLongitude(
                request.longitude()
        );

        location.setSpeed(
                request.speed()
        );

        location.setHeading(
                request.heading()
        );

        location.setRecordedAt(
                request.recordedAt()
        );

        VehicleLocation saved =
                locationRepository
                        .saveAndFlush(
                                location
                        );

        VehicleLocationResponse response =
                toResponse(saved);

        eventPublisher.publishEvent(
                new VehicleLocationCreatedEvent(
                        response
                )
        );

        return response;
    }

    @Cacheable(
            cacheNames = CacheNames.LATEST_VEHICLE_LOCATION,
            key = "#vehicleId"
    )
    @Transactional(readOnly = true)
    public VehicleLocationResponse getLatest(
            Long vehicleId
    ) {
        getVehicle(
                vehicleId
        );

        VehicleLocation location =
                locationRepository
                        .findFirstByVehicle_IdOrderByRecordedAtDescIdDesc(
                                vehicleId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Vehicle "
                                                + vehicleId
                                                + " has no location data"
                                )
                        );

        return toResponse(
                location
        );
    }

    @Transactional(readOnly = true)
    public VehicleLocationHistoryResponse getHistory(
            Long vehicleId,
            Instant cursorRecordedAt,
            Long cursorId,
            int size
    ) {
        getVehicle(
                vehicleId
        );

        validateHistoryRequest(
                cursorRecordedAt,
                cursorId,
                size
        );

        PageRequest pageable =
                PageRequest.of(
                        0,
                        size
                );

        Slice<VehicleLocation> slice;

        if (cursorRecordedAt == null) {

            slice =
                    locationRepository
                            .findByVehicle_IdOrderByRecordedAtDescIdDesc(
                                    vehicleId,
                                    pageable
                            );

        } else {

            slice =
                    locationRepository
                            .findAfterCursor(
                                    vehicleId,
                                    cursorRecordedAt,
                                    cursorId,
                                    pageable
                            );
        }

        List<VehicleLocation> locations =
                slice.getContent();

        List<VehicleLocationResponse> items =
                locations.stream()
                        .map(this::toResponse)
                        .toList();

        VehicleLocationCursor nextCursor =
                createNextCursor(
                        locations,
                        slice.hasNext()
                );

        return new VehicleLocationHistoryResponse(
                items,
                nextCursor,
                slice.hasNext()
        );
    }

    private Vehicle getVehicle(
            Long vehicleId
    ) {
        return vehicleRepository
                .findById(
                        vehicleId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Vehicle with id "
                                        + vehicleId
                                        + " was not found"
                        )
                );
    }

    private void validateVehicle(
            Vehicle vehicle
    ) {
        if (vehicle.getStatus()
                == VehicleStatus.RETIRED) {

            throw new BusinessRuleViolationException(
                    "Location cannot be recorded for retired vehicle "
                            + vehicle.getId()
            );
        }
    }

    private void validateRecordedAt(
            Instant recordedAt
    ) {
        Instant maximumAllowed =
                Instant.now()
                        .plusSeconds(
                                MAX_FUTURE_CLOCK_SKEW_SECONDS
                        );

        if (recordedAt.isAfter(
                maximumAllowed
        )) {
            throw new IllegalArgumentException(
                    "recordedAt cannot be more than 5 minutes in the future"
            );
        }
    }

    private void validateHistoryRequest(
            Instant cursorRecordedAt,
            Long cursorId,
            int size
    ) {
        if (size < 1
                || size > MAX_PAGE_SIZE) {

            throw new IllegalArgumentException(
                    "Page size must be between 1 and "
                            + MAX_PAGE_SIZE
            );
        }

        boolean recordedAtProvided =
                cursorRecordedAt != null;

        boolean idProvided =
                cursorId != null;

        if (recordedAtProvided
                != idProvided) {

            throw new IllegalArgumentException(
                    "cursorRecordedAt and cursorId must be provided together"
            );
        }

        if (cursorId != null
                && cursorId <= 0) {

            throw new IllegalArgumentException(
                    "cursorId must be greater than zero"
            );
        }
    }

    private VehicleLocationResponse toResponse(
            VehicleLocation location
    ) {
        return new VehicleLocationResponse(
                location.getId(),
                location.getVehicle().getId(),
                location.getLatitude(),
                location.getLongitude(),
                location.getSpeed(),
                location.getHeading(),
                location.getRecordedAt(),
                location.getCreatedAt()
        );
    }

    private VehicleLocationCursor createNextCursor(
            List<VehicleLocation> locations,
            boolean hasNext
    ) {
        if (!hasNext
                || locations.isEmpty()) {

            return null;
        }

        VehicleLocation last =
                locations.get(
                        locations.size() - 1
                );

        return new VehicleLocationCursor(
                last.getRecordedAt(),
                last.getId()
        );
    }
}