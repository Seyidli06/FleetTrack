package com.fleettrack.maintenance;

import com.fleettrack.common.exception.StaleVersionException;
import com.fleettrack.integration.AbstractIntegrationTest;
import com.fleettrack.maintenance.dto.ChangeMaintenanceStatusRequest;
import com.fleettrack.maintenance.dto.MaintenanceResponse;
import com.fleettrack.maintenance.entity.MaintenanceRecord;
import com.fleettrack.maintenance.entity.MaintenanceStatus;
import com.fleettrack.maintenance.repository.MaintenanceRepository;
import com.fleettrack.maintenance.service.MaintenanceService;
import com.fleettrack.vehicle.entity.Vehicle;
import com.fleettrack.vehicle.entity.VehicleStatus;
import com.fleettrack.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
class MaintenanceStaleVersionIntegrationTest
        extends AbstractIntegrationTest {

    @Autowired
    private MaintenanceService maintenanceService;

    @Autowired
    private MaintenanceRepository maintenanceRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @AfterEach
    void cleanup() {

        maintenanceRepository.deleteAll();
        maintenanceRepository.flush();

        vehicleRepository.deleteAll();
        vehicleRepository.flush();
    }

    @Test
    void shouldRejectSecondChangeWhenClientUsesStaleVersion() {

        Vehicle vehicle =
                createVehicle();

        MaintenanceRecord maintenance =
                createScheduledMaintenance(
                        vehicle
                );

        Long maintenanceId =
                maintenance.getId();

        Long originalVersion =
                maintenance.getVersion();

        assertThat(originalVersion)
                .isEqualTo(0L);


        MaintenanceResponse firstResponse =
                maintenanceService.changeStatus(
                        maintenanceId,
                        new ChangeMaintenanceStatusRequest(
                                MaintenanceStatus.IN_PROGRESS,
                                originalVersion
                        )
                );

        assertThat(
                firstResponse.status()
        )
                .isEqualTo(
                        MaintenanceStatus.IN_PROGRESS
                );


        MaintenanceRecord updated =
                maintenanceRepository
                        .findById(
                                maintenanceId
                        )
                        .orElseThrow();

        Long currentVersion =
                updated.getVersion();

        assertThat(currentVersion)
                .isGreaterThan(
                        originalVersion
                );


        assertThatThrownBy(() ->
                maintenanceService.changeStatus(
                        maintenanceId,
                        new ChangeMaintenanceStatusRequest(
                                MaintenanceStatus.CANCELLED,
                                originalVersion
                        )
                )
        )
                .isInstanceOf(
                        StaleVersionException.class
                )
                .hasMessageContaining(
                        "Maintenance has been modified"
                );


        MaintenanceRecord finalRecord =
                maintenanceRepository
                        .findById(
                                maintenanceId
                        )
                        .orElseThrow();

        assertThat(
                finalRecord.getStatus()
        )
                .isEqualTo(
                        MaintenanceStatus.IN_PROGRESS
                );

        assertThat(
                finalRecord.getVersion()
        )
                .isEqualTo(
                        currentVersion
                );


        Vehicle finalVehicle =
                vehicleRepository
                        .findById(
                                vehicle.getId()
                        )
                        .orElseThrow();

        assertThat(
                finalVehicle.getStatus()
        )
                .isEqualTo(
                        VehicleStatus.IN_MAINTENANCE
                );
    }

    private Vehicle createVehicle() {

        Vehicle vehicle =
                new Vehicle();

        vehicle.setVin(
                "TESTVIN0000000003"
        );

        vehicle.setLicensePlate(
                "99-TC-003"
        );

        vehicle.setMake(
                "Test"
        );

        vehicle.setModel(
                "OptimisticLock"
        );

        vehicle.setManufactureYear(
                2026
        );

        vehicle.setStatus(
                VehicleStatus.ACTIVE
        );

        return vehicleRepository
                .saveAndFlush(
                        vehicle
                );
    }

    private MaintenanceRecord createScheduledMaintenance(
            Vehicle vehicle
    ) {

        MaintenanceRecord record =
                new MaintenanceRecord();

        record.setVehicle(
                vehicle
        );

        record.setMaintenanceType(
                "OPTIMISTIC_LOCK_TEST"
        );

        record.setDescription(
                "Phase 17 stale version integration test"
        );

        record.setServiceDate(
                LocalDate.now()
        );

        record.setNextServiceDate(
                LocalDate.now()
                        .plusMonths(6)
        );

        record.setOdometer(
                30_000L
        );

        record.setCost(
                BigDecimal.valueOf(
                        200
                )
        );

        record.setStatus(
                MaintenanceStatus.SCHEDULED
        );

        return maintenanceRepository
                .saveAndFlush(
                        record
                );
    }
}