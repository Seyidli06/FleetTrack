package com.fleettrack.maintenance;

import com.fleettrack.common.exception.BusinessRuleViolationException;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
class MaintenanceServiceConcurrencyIntegrationTest
        extends AbstractIntegrationTest {

    @Autowired
    private MaintenanceService maintenanceService;

    @Autowired
    private MaintenanceRepository maintenanceRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanup() {

        /*
         * FK sırasına görə əvvəl maintenance,
         * sonra vehicle silirik.
         */
        maintenanceRepository.deleteAll();
        maintenanceRepository.flush();

        vehicleRepository.deleteAll();
        vehicleRepository.flush();
    }

    @Test
    void shouldAllowOnlyOneMaintenanceToStartWhenServiceRequestsRace()
            throws Exception {

        Vehicle vehicle =
                createVehicle();

        MaintenanceRecord first =
                createScheduledMaintenance(
                        vehicle,
                        "SERVICE_RACE_A"
                );

        MaintenanceRecord second =
                createScheduledMaintenance(
                        vehicle,
                        "SERVICE_RACE_B"
                );

        Long vehicleId =
                vehicle.getId();

        Long firstMaintenanceId =
                first.getId();

        Long secondMaintenanceId =
                second.getId();

        Long firstVersion =
                first.getVersion();

        Long secondVersion =
                second.getVersion();

        /*
         * Hər iki worker əvvəl start xəttinə gələcək.
         */
        CountDownLatch readyLatch =
                new CountDownLatch(2);

        /*
         * Test thread hər iki worker hazır olduqdan
         * sonra onları eyni anda buraxacaq.
         */
        CountDownLatch startLatch =
                new CountDownLatch(1);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        try {

            Future<ServiceRaceResult> firstFuture =
                    executor.submit(() ->
                            invokeChangeStatus(
                                    firstMaintenanceId,
                                    firstVersion,
                                    readyLatch,
                                    startLatch
                            )
                    );

            Future<ServiceRaceResult> secondFuture =
                    executor.submit(() ->
                            invokeChangeStatus(
                                    secondMaintenanceId,
                                    secondVersion,
                                    readyLatch,
                                    startLatch
                            )
                    );

            boolean bothReady =
                    readyLatch.await(
                            10,
                            TimeUnit.SECONDS
                    );

            assertThat(bothReady)
                    .as(
                            "Both service requests should be ready before race starts"
                    )
                    .isTrue();

            /*
             * İki request mümkün qədər eyni anda
             * changeStatus() çağırmağa başlayır.
             */
            startLatch.countDown();

            ServiceRaceResult firstResult =
                    firstFuture.get(
                            15,
                            TimeUnit.SECONDS
                    );

            ServiceRaceResult secondResult =
                    secondFuture.get(
                            15,
                            TimeUnit.SECONDS
                    );

            List<ServiceRaceResult> results =
                    List.of(
                            firstResult,
                            secondResult
                    );

            long successCount =
                    results.stream()
                            .filter(
                                    ServiceRaceResult::success
                            )
                            .count();

            long failureCount =
                    results.stream()
                            .filter(result ->
                                    !result.success()
                            )
                            .count();

            /*
             * Əsas business invariant:
             *
             * iki maintenance eyni vehicle üçün
             * eyni anda başlaya bilməz.
             */
            assertThat(successCount)
                    .isEqualTo(1);

            assertThat(failureCount)
                    .isEqualTo(1);

            ServiceRaceResult successfulResult =
                    results.stream()
                            .filter(
                                    ServiceRaceResult::success
                            )
                            .findFirst()
                            .orElseThrow();

            assertThat(
                    successfulResult.response()
            )
                    .isNotNull();

            assertThat(
                    successfulResult
                            .response()
                            .status()
            )
                    .isEqualTo(
                            MaintenanceStatus.IN_PROGRESS
                    );

            /*
             * Race-in dəqiq timing-indən asılı olaraq
             * ikinci transaction bir neçə qoruma qatından
             * birində uduza bilər:
             *
             * 1. service business validation
             * 2. PostgreSQL partial unique index
             * 3. vehicle optimistic locking
             */
            ServiceRaceResult failedResult =
                    results.stream()
                            .filter(result ->
                                    !result.success()
                            )
                            .findFirst()
                            .orElseThrow();

            assertThat(
                    failedResult.error()
            )
                    .isNotNull();

            assertThat(
                    isExpectedConcurrencyFailure(
                            failedResult.error()
                    )
            )
                    .as(
                            "Failure should come from a known concurrency protection layer, but was: "
                                    + failedResult.error()
                                    .getClass()
                                    .getName()
                    )
                    .isTrue();

            /*
             * DB invariant:
             *
             * eyni vehicle üçün yalnız bir
             * IN_PROGRESS maintenance.
             */
            Integer inProgressCount =
                    jdbcTemplate.queryForObject(
                            """
                            SELECT COUNT(*)
                            FROM maintenance_records
                            WHERE vehicle_id = ?
                              AND status = 'IN_PROGRESS'
                            """,
                            Integer.class,
                            vehicleId
                    );

            assertThat(inProgressCount)
                    .isEqualTo(1);

            /*
             * İki maintenance-dən biri başlamalı,
             * digəri rollback/business rejection
             * nəticəsində SCHEDULED qalmalıdır.
             */
            List<String> statuses =
                    jdbcTemplate.queryForList(
                            """
                            SELECT status
                            FROM maintenance_records
                            WHERE id IN (?, ?)
                            ORDER BY id
                            """,
                            String.class,
                            firstMaintenanceId,
                            secondMaintenanceId
                    );

            assertThat(statuses)
                    .containsExactlyInAnyOrder(
                            "IN_PROGRESS",
                            "SCHEDULED"
                    );

            /*
             * Uğurlu maintenance başladığı üçün
             * vehicle artıq maintenance vəziyyətindədir.
             */
            String vehicleStatus =
                    jdbcTemplate.queryForObject(
                            """
                            SELECT status
                            FROM vehicles
                            WHERE id = ?
                            """,
                            String.class,
                            vehicleId
                    );

            assertThat(vehicleStatus)
                    .isEqualTo(
                            VehicleStatus
                                    .IN_MAINTENANCE
                                    .name()
                    );
        }
        finally {

            startLatch.countDown();

            executor.shutdownNow();

            executor.awaitTermination(
                    5,
                    TimeUnit.SECONDS
            );
        }
    }

    private ServiceRaceResult invokeChangeStatus(
            Long maintenanceId,
            Long version,
            CountDownLatch readyLatch,
            CountDownLatch startLatch
    ) {

        try {

            /*
             * Worker start xəttinə çatdı.
             */
            readyLatch.countDown();

            boolean started =
                    startLatch.await(
                            10,
                            TimeUnit.SECONDS
                    );

            if (!started) {

                throw new IllegalStateException(
                        "Service concurrency start barrier timed out"
                );
            }

            ChangeMaintenanceStatusRequest request =
                    new ChangeMaintenanceStatusRequest(
                            MaintenanceStatus.IN_PROGRESS,
                            version
                    );

            MaintenanceResponse response =
                    maintenanceService
                            .changeStatus(
                                    maintenanceId,
                                    request
                            );

            return ServiceRaceResult
                    .successResult(
                            response
                    );
        }
        catch (
                InterruptedException exception
        ) {

            Thread.currentThread()
                    .interrupt();

            return ServiceRaceResult
                    .failureResult(
                            new IllegalStateException(
                                    "Service concurrency test was interrupted",
                                    exception
                            )
                    );
        }
        catch (
                RuntimeException exception
        ) {

            return ServiceRaceResult
                    .failureResult(
                            exception
                    );
        }
    }

    private boolean isExpectedConcurrencyFailure(
            Throwable throwable
    ) {

        Throwable current =
                throwable;

        while (current != null) {

            if (current
                    instanceof BusinessRuleViolationException) {

                return true;
            }

            if (current
                    instanceof DataIntegrityViolationException) {

                return true;
            }

            if (current
                    instanceof ObjectOptimisticLockingFailureException) {

                return true;
            }

            current =
                    current.getCause();
        }

        return false;
    }

    private Vehicle createVehicle() {

        Vehicle vehicle =
                new Vehicle();

        vehicle.setVin(
                "TESTVIN0000000002"
        );

        vehicle.setLicensePlate(
                "99-TC-002"
        );

        vehicle.setMake(
                "Test"
        );

        vehicle.setModel(
                "ServiceConcurrency"
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
            Vehicle vehicle,
            String type
    ) {

        MaintenanceRecord record =
                new MaintenanceRecord();

        record.setVehicle(
                vehicle
        );

        record.setMaintenanceType(
                type
        );

        record.setDescription(
                "Phase 17 service concurrency test"
        );

        record.setServiceDate(
                LocalDate.now()
        );

        record.setNextServiceDate(
                LocalDate.now()
                        .plusMonths(6)
        );

        record.setOdometer(
                20_000L
        );

        record.setCost(
                BigDecimal.valueOf(
                        150
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

    private record ServiceRaceResult(
            boolean success,
            MaintenanceResponse response,
            RuntimeException error
    ) {

        private static ServiceRaceResult successResult(
                MaintenanceResponse response
        ) {

            return new ServiceRaceResult(
                    true,
                    response,
                    null
            );
        }

        private static ServiceRaceResult failureResult(
                RuntimeException error
        ) {

            return new ServiceRaceResult(
                    false,
                    null,
                    error
            );
        }
    }
}