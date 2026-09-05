package com.fleettrack.maintenance.service;

import com.fleettrack.common.cache.CacheNames;
import com.fleettrack.common.dto.PageResponse;
import com.fleettrack.common.exception.BusinessRuleViolationException;
import com.fleettrack.common.exception.ResourceNotFoundException;
import com.fleettrack.common.exception.StaleVersionException;
import com.fleettrack.maintenance.dto.ChangeMaintenanceStatusRequest;
import com.fleettrack.maintenance.dto.CreateMaintenanceRequest;
import com.fleettrack.maintenance.dto.MaintenanceFilter;
import com.fleettrack.maintenance.dto.MaintenanceResponse;
import com.fleettrack.maintenance.dto.UpdateMaintenanceRequest;
import com.fleettrack.maintenance.entity.MaintenanceRecord;
import com.fleettrack.maintenance.entity.MaintenanceStatus;
import com.fleettrack.maintenance.repository.MaintenanceRepository;
import com.fleettrack.maintenance.specification.MaintenanceSpecification;
import com.fleettrack.vehicle.entity.Vehicle;
import com.fleettrack.vehicle.entity.VehicleStatus;
import com.fleettrack.vehicle.repository.VehicleRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

@Service
public class MaintenanceService {

    private static final int MAX_PAGE_SIZE = 100;

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of(
                    "id",
                    "maintenanceType",
                    "serviceDate",
                    "nextServiceDate",
                    "odometer",
                    "cost",
                    "status",
                    "createdAt",
                    "updatedAt"
            );

    private final MaintenanceRepository maintenanceRepository;
    private final VehicleRepository vehicleRepository;

    public MaintenanceService(
            MaintenanceRepository maintenanceRepository,
            VehicleRepository vehicleRepository
    ) {
        this.maintenanceRepository =
                maintenanceRepository;

        this.vehicleRepository =
                vehicleRepository;
    }

    @Transactional
    public MaintenanceResponse create(
            Long vehicleId,
            CreateMaintenanceRequest request
    ) {
        Vehicle vehicle =
                getVehicle(
                        vehicleId
                );

        if (vehicle.getStatus()
                == VehicleStatus.RETIRED) {

            throw new BusinessRuleViolationException(
                    "Maintenance cannot be created for retired vehicle "
                            + vehicleId
            );
        }

        validateDates(
                request.serviceDate(),
                request.nextServiceDate()
        );

        MaintenanceRecord record =
                new MaintenanceRecord();

        record.setVehicle(
                vehicle
        );

        record.setMaintenanceType(
                normalizeRequired(
                        request.maintenanceType()
                )
        );

        record.setDescription(
                normalizeOptional(
                        request.description()
                )
        );

        record.setServiceDate(
                request.serviceDate()
        );

        record.setNextServiceDate(
                request.nextServiceDate()
        );

        record.setOdometer(
                request.odometer()
        );

        record.setCost(
                request.cost()
        );

        record.setStatus(
                MaintenanceStatus.SCHEDULED
        );

        MaintenanceRecord saved =
                maintenanceRepository
                        .saveAndFlush(
                                record
                        );

        return toResponse(
                saved
        );
    }

    @Transactional(readOnly = true)
    public MaintenanceResponse getById(
            Long id
    ) {
        return toResponse(
                maintenanceRepository
                        .findDetailedById(id)
                        .orElseThrow(() ->
                                maintenanceNotFound(
                                        id
                                )
                        )
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<MaintenanceResponse> getVehicleHistory(
            Long vehicleId,
            MaintenanceFilter filter,
            int page,
            int size,
            String sort
    ) {
        getVehicle(
                vehicleId
        );

        validatePagination(
                page,
                size
        );

        validateFilter(
                filter
        );

        PageRequest pageable =
                PageRequest.of(
                        page,
                        size,
                        parseSort(
                                sort
                        )
                );

        Page<MaintenanceResponse> result =
                maintenanceRepository
                        .findAll(
                                MaintenanceSpecification
                                        .withFilter(
                                                vehicleId,
                                                filter
                                        ),
                                pageable
                        )
                        .map(
                                this::toResponse
                        );

        return PageResponse.from(
                result
        );
    }

    @Transactional
    public MaintenanceResponse update(
            Long id,
            UpdateMaintenanceRequest request
    ) {
        MaintenanceRecord record =
                getForUpdate(
                        id
                );

        ensureVersionMatches(
                record,
                request.version()
        );

        ensureEditable(
                record
        );

        validateDates(
                request.serviceDate(),
                request.nextServiceDate()
        );

        record.setMaintenanceType(
                normalizeRequired(
                        request.maintenanceType()
                )
        );

        record.setDescription(
                normalizeOptional(
                        request.description()
                )
        );

        record.setServiceDate(
                request.serviceDate()
        );

        record.setNextServiceDate(
                request.nextServiceDate()
        );

        record.setOdometer(
                request.odometer()
        );

        record.setCost(
                request.cost()
        );

        maintenanceRepository.flush();

        return toResponse(
                record
        );
    }

    @CacheEvict(
            cacheNames = CacheNames.VEHICLE_BY_ID,
            key = "#result.vehicleId()"
    )
    @Transactional
    public MaintenanceResponse changeStatus(
            Long id,
            ChangeMaintenanceStatusRequest request
    ) {
        MaintenanceRecord record =
                getForUpdate(
                        id
                );

        ensureVersionMatches(
                record,
                request.version()
        );

        MaintenanceStatus currentStatus =
                record.getStatus();

        MaintenanceStatus targetStatus =
                request.status();

        if (currentStatus == targetStatus) {

            throw new BusinessRuleViolationException(
                    "Maintenance is already in status "
                            + targetStatus
            );
        }

        validateTransition(
                currentStatus,
                targetStatus
        );

        Vehicle vehicle =
                record.getVehicle();

        if (targetStatus
                == MaintenanceStatus.IN_PROGRESS) {

            startMaintenance(
                    vehicle
            );
        }

        if (targetStatus
                == MaintenanceStatus.COMPLETED) {

            finishInProgressMaintenance(
                    vehicle
            );
        }

        if (targetStatus
                == MaintenanceStatus.CANCELLED
                && currentStatus
                == MaintenanceStatus.IN_PROGRESS) {

            finishInProgressMaintenance(
                    vehicle
            );
        }

        record.setStatus(
                targetStatus
        );

        maintenanceRepository.flush();
        vehicleRepository.flush();

        return toResponse(
                record
        );
    }

    @Transactional
    public void delete(
            Long id
    ) {
        MaintenanceRecord record =
                getForUpdate(
                        id
                );

        if (record.getStatus()
                != MaintenanceStatus.SCHEDULED) {

            throw new BusinessRuleViolationException(
                    "Only SCHEDULED maintenance can be deleted"
            );
        }

        maintenanceRepository.delete(
                record
        );

        maintenanceRepository.flush();
    }

    private void startMaintenance(
            Vehicle vehicle
    ) {
        if (vehicle.getStatus()
                != VehicleStatus.ACTIVE) {

            throw new BusinessRuleViolationException(
                    "Vehicle "
                            + vehicle.getId()
                            + " cannot start maintenance while status is "
                            + vehicle.getStatus()
            );
        }

        boolean anotherMaintenanceInProgress =
                maintenanceRepository
                        .existsByVehicle_IdAndStatus(
                                vehicle.getId(),
                                MaintenanceStatus.IN_PROGRESS
                        );

        if (anotherMaintenanceInProgress) {

            throw new BusinessRuleViolationException(
                    "Vehicle "
                            + vehicle.getId()
                            + " already has an IN_PROGRESS maintenance"
            );
        }

        vehicle.setStatus(
                VehicleStatus.IN_MAINTENANCE
        );
    }

    private void finishInProgressMaintenance(
            Vehicle vehicle
    ) {
        if (vehicle.getStatus()
                == VehicleStatus.IN_MAINTENANCE) {

            vehicle.setStatus(
                    VehicleStatus.ACTIVE
            );
        }
    }

    private void validateTransition(
            MaintenanceStatus current,
            MaintenanceStatus target
    ) {
        boolean allowed =
                switch (current) {

                    case SCHEDULED ->
                            target
                                    == MaintenanceStatus.IN_PROGRESS
                                    || target
                                    == MaintenanceStatus.CANCELLED;

                    case IN_PROGRESS ->
                            target
                                    == MaintenanceStatus.COMPLETED
                                    || target
                                    == MaintenanceStatus.CANCELLED;

                    case COMPLETED, CANCELLED ->
                            false;
                };

        if (!allowed) {

            throw new BusinessRuleViolationException(
                    "Maintenance status cannot change from "
                            + current
                            + " to "
                            + target
            );
        }
    }

    private void ensureEditable(
            MaintenanceRecord record
    ) {
        if (record.getStatus()
                == MaintenanceStatus.COMPLETED
                || record.getStatus()
                == MaintenanceStatus.CANCELLED) {

            throw new BusinessRuleViolationException(
                    "Terminal maintenance record cannot be modified"
            );
        }
    }

    private void ensureVersionMatches(
            MaintenanceRecord record,
            Long requestedVersion
    ) {
        if (!record.getVersion()
                .equals(
                        requestedVersion
                )) {

            throw new StaleVersionException(
                    "Maintenance has been modified. "
                            + "Expected version "
                            + record.getVersion()
                            + " but received "
                            + requestedVersion
            );
        }
    }

    private void validateDates(
            LocalDate serviceDate,
            LocalDate nextServiceDate
    ) {
        if (nextServiceDate != null
                && nextServiceDate
                .isBefore(
                        serviceDate
                )) {

            throw new IllegalArgumentException(
                    "nextServiceDate cannot be before serviceDate"
            );
        }
    }

    private void validateFilter(
            MaintenanceFilter filter
    ) {
        if (filter == null) {
            return;
        }

        if (filter.serviceDateFrom() != null
                && filter.serviceDateTo() != null
                && filter.serviceDateFrom()
                .isAfter(
                        filter.serviceDateTo()
                )) {

            throw new IllegalArgumentException(
                    "serviceDateFrom cannot be after serviceDateTo"
            );
        }
    }

    private void validatePagination(
            int page,
            int size
    ) {
        if (page < 0) {

            throw new IllegalArgumentException(
                    "Page must be zero or greater"
            );
        }

        if (size < 1
                || size > MAX_PAGE_SIZE) {

            throw new IllegalArgumentException(
                    "Page size must be between 1 and "
                            + MAX_PAGE_SIZE
            );
        }
    }

    private Sort parseSort(
            String sort
    ) {
        if (sort == null
                || sort.isBlank()) {

            return Sort.by(
                    Sort.Direction.DESC,
                    "serviceDate"
            );
        }

        String[] parts =
                sort.split(",");

        if (parts.length > 2) {

            throw new IllegalArgumentException(
                    "Sort format must be field,direction"
            );
        }

        String property =
                parts[0].trim();

        if (property.isBlank()) {

            throw new IllegalArgumentException(
                    "Sort field cannot be blank"
            );
        }

        if (!ALLOWED_SORT_FIELDS
                .contains(
                        property
                )) {

            throw new IllegalArgumentException(
                    "Unsupported sort field: "
                            + property
            );
        }

        Sort.Direction direction =
                Sort.Direction.ASC;

        if (parts.length == 2) {
            try {
                direction =
                        Sort.Direction.fromString(
                                parts[1].trim()
                        );
            } catch (
                    IllegalArgumentException exception
            ) {
                throw new IllegalArgumentException(
                        "Sort direction must be ASC or DESC"
                );
            }
        }

        return Sort.by(
                direction,
                property
        );
    }

    private Vehicle getVehicle(
            Long id
    ) {
        return vehicleRepository
                .findById(
                        id
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Vehicle with id "
                                        + id
                                        + " was not found"
                        )
                );
    }

    private MaintenanceRecord getForUpdate(
            Long id
    ) {
        return maintenanceRepository
                .findForUpdate(
                        id
                )
                .orElseThrow(() ->
                        maintenanceNotFound(
                                id
                        )
                );
    }

    private ResourceNotFoundException maintenanceNotFound(
            Long id
    ) {
        return new ResourceNotFoundException(
                "Maintenance with id "
                        + id
                        + " was not found"
        );
    }

    private MaintenanceResponse toResponse(
            MaintenanceRecord record
    ) {
        Vehicle vehicle =
                record.getVehicle();

        return new MaintenanceResponse(
                record.getId(),

                vehicle.getId(),
                vehicle.getVin(),
                vehicle.getLicensePlate(),

                record.getMaintenanceType(),
                record.getDescription(),

                record.getServiceDate(),
                record.getNextServiceDate(),

                record.getOdometer(),
                record.getCost(),

                record.getStatus(),

                record.getCreatedAt(),
                record.getUpdatedAt(),

                record.getVersion()
        );
    }

    private String normalizeRequired(
            String value
    ) {
        return value.trim();
    }

    private String normalizeOptional(
            String value
    ) {
        if (value == null
                || value.isBlank()) {

            return null;
        }

        return value.trim();
    }
}