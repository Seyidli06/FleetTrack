package com.fleettrack.assignment.service;

import com.fleettrack.assignment.dto.AssignVehicleRequest;
import com.fleettrack.assignment.dto.VehicleAssignmentResponse;
import com.fleettrack.assignment.entity.VehicleAssignment;
import com.fleettrack.assignment.repository.VehicleAssignmentRepository;
import com.fleettrack.common.exception.BusinessRuleViolationException;
import com.fleettrack.common.exception.ResourceNotFoundException;
import com.fleettrack.driver.entity.Driver;
import com.fleettrack.driver.entity.DriverStatus;
import com.fleettrack.driver.repository.DriverRepository;
import com.fleettrack.vehicle.entity.Vehicle;
import com.fleettrack.vehicle.entity.VehicleStatus;
import com.fleettrack.vehicle.repository.VehicleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

@Service
public class VehicleAssignmentService {

    private static final int MAX_PAGE_SIZE = 100;

    private final VehicleAssignmentRepository assignmentRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;

    public VehicleAssignmentService(
            VehicleAssignmentRepository assignmentRepository,
            VehicleRepository vehicleRepository,
            DriverRepository driverRepository
    ) {
        this.assignmentRepository = assignmentRepository;
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
    }

    @Transactional
    public VehicleAssignmentResponse assign(
            Long vehicleId,
            AssignVehicleRequest request
    ) {
        Vehicle vehicle = getVehicle(vehicleId);
        Driver driver = getDriver(request.driverId());

        validateVehicleForAssignment(vehicle);
        validateDriverForAssignment(driver);

        if (assignmentRepository
                .existsByVehicle_IdAndUnassignedAtIsNull(vehicleId)) {

            throw new BusinessRuleViolationException(
                    "Vehicle " + vehicleId
                            + " already has an active driver"
            );
        }

        if (assignmentRepository
                .existsByDriver_IdAndUnassignedAtIsNull(driver.getId())) {

            throw new BusinessRuleViolationException(
                    "Driver " + driver.getId()
                            + " is already assigned to another vehicle"
            );
        }

        VehicleAssignment assignment = new VehicleAssignment();

        assignment.setVehicle(vehicle);
        assignment.setDriver(driver);

        VehicleAssignment saved =
                assignmentRepository.saveAndFlush(assignment);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public VehicleAssignmentResponse getCurrent(
            Long vehicleId
    ) {
        getVehicle(vehicleId);

        VehicleAssignment assignment =
                assignmentRepository
                        .findByVehicle_IdAndUnassignedAtIsNull(vehicleId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Vehicle "
                                                + vehicleId
                                                + " has no active assignment"
                                )
                        );

        return toResponse(assignment);
    }

    @Transactional(readOnly = true)
    public Page<VehicleAssignmentResponse> getHistory(
            Long vehicleId,
            int page,
            int size
    ) {
        getVehicle(vehicleId);

        validatePagination(page, size);

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Direction.DESC,
                        "assignedAt"
                )
        );

        return assignmentRepository
                .findByVehicle_Id(
                        vehicleId,
                        pageable
                )
                .map(this::toResponse);
    }

    @Transactional
    public VehicleAssignmentResponse unassign(
            Long vehicleId
    ) {
        getVehicle(vehicleId);

        VehicleAssignment assignment =
                assignmentRepository
                        .findActiveForUpdate(vehicleId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Vehicle "
                                                + vehicleId
                                                + " has no active assignment"
                                )
                        );

        assignment.setUnassignedAt(
                Instant.now()
        );

        assignmentRepository.flush();

        return toResponse(assignment);
    }

    private Vehicle getVehicle(Long id) {
        return vehicleRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Vehicle with id "
                                        + id
                                        + " was not found"
                        )
                );
    }

    private Driver getDriver(Long id) {
        return driverRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Driver with id "
                                        + id
                                        + " was not found"
                        )
                );
    }

    private void validateVehicleForAssignment(
            Vehicle vehicle
    ) {
        if (vehicle.getStatus() != VehicleStatus.ACTIVE) {
            throw new BusinessRuleViolationException(
                    "Vehicle "
                            + vehicle.getId()
                            + " cannot be assigned while status is "
                            + vehicle.getStatus()
            );
        }
    }

    private void validateDriverForAssignment(
            Driver driver
    ) {
        if (driver.getStatus() != DriverStatus.ACTIVE) {
            throw new BusinessRuleViolationException(
                    "Driver "
                            + driver.getId()
                            + " cannot be assigned while status is "
                            + driver.getStatus()
            );
        }

        if (driver.getLicenseExpiryDate()
                .isBefore(LocalDate.now())) {

            throw new BusinessRuleViolationException(
                    "Driver "
                            + driver.getId()
                            + " has an expired license"
            );
        }
    }

    private VehicleAssignmentResponse toResponse(
            VehicleAssignment assignment
    ) {
        Vehicle vehicle = assignment.getVehicle();
        Driver driver = assignment.getDriver();

        return new VehicleAssignmentResponse(
                assignment.getId(),

                vehicle.getId(),
                vehicle.getVin(),
                vehicle.getLicensePlate(),

                driver.getId(),
                driver.getFirstName()
                        + " "
                        + driver.getLastName(),

                assignment.getAssignedAt(),
                assignment.getUnassignedAt(),

                assignment.isActive()
        );
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

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and "
                            + MAX_PAGE_SIZE
            );
        }
    }
}