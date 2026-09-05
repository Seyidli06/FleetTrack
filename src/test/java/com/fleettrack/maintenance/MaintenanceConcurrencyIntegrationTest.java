package com.fleettrack.maintenance;

import com.fleettrack.integration.AbstractIntegrationTest;
import com.fleettrack.maintenance.entity.MaintenanceRecord;
import com.fleettrack.maintenance.entity.MaintenanceStatus;
import com.fleettrack.maintenance.repository.MaintenanceRepository;
import com.fleettrack.vehicle.entity.Vehicle;
import com.fleettrack.vehicle.entity.VehicleStatus;
import com.fleettrack.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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
class MaintenanceConcurrencyIntegrationTest
        extends AbstractIntegrationTest {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private MaintenanceRepository maintenanceRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanup() {

        maintenanceRepository.deleteAll();
        maintenanceRepository.flush();

        vehicleRepository.deleteAll();
        vehicleRepository.flush();
    }

    @Test
    void shouldAllowOnlyOneInProgressMaintenancePerVehicleWhenRequestsRace()
            throws Exception {

        Vehicle vehicle =
                createVehicle();

        MaintenanceRecord first =
                createScheduledMaintenance(
                        vehicle,
                        "CONCURRENCY_TEST_A"
                );

        MaintenanceRecord second =
                createScheduledMaintenance(
                        vehicle,
                        "CONCURRENCY_TEST_B"
                );

        Long vehicleId =
                vehicle.getId();

        Long firstMaintenanceId =
                first.getId();

        Long secondMaintenanceId =
                second.getId();

        /*
         * Hər iki worker thread-in transaction daxilində
         * maintenance record-u oxumasını gözləyirik.
         */
        CountDownLatch readyLatch =
                new CountDownLatch(2);

        /*
         * Hər iki transaction flush etməzdən əvvəl
         * eyni barrier-də görüşəcək.
         *
         * Məqsəd odur ki, hər ikisi mümkün qədər
         * eyni anda IN_PROGRESS yazmağa çalışsın.
         */
        CountDownLatch flushLatch =
                new CountDownLatch(2);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        try {

            Future<RaceResult> firstFuture =
                    executor.submit(() ->
                            moveToInProgress(
                                    firstMaintenanceId,
                                    readyLatch,
                                    flushLatch
                            )
                    );

            Future<RaceResult> secondFuture =
                    executor.submit(() ->
                            moveToInProgress(
                                    secondMaintenanceId,
                                    readyLatch,
                                    flushLatch
                            )
                    );

            boolean bothReady =
                    readyLatch.await(
                            10,
                            TimeUnit.SECONDS
                    );

            assertThat(bothReady)
                    .as(
                            "Both concurrent transactions should become ready"
                    )
                    .isTrue();

            RaceResult firstResult =
                    firstFuture.get(
                            15,
                            TimeUnit.SECONDS
                    );

            RaceResult secondResult =
                    secondFuture.get(
                            15,
                            TimeUnit.SECONDS
                    );

            List<RaceResult> results =
                    List.of(
                            firstResult,
                            secondResult
                    );

            long successCount =
                    results.stream()
                            .filter(
                                    RaceResult::success
                            )
                            .count();

            long failureCount =
                    results.stream()
                            .filter(result ->
                                    !result.success()
                            )
                            .count();

            /*
             * Əsas concurrency invariant:
             *
             * iki transaction-dan yalnız biri
             * IN_PROGRESS ola bilməlidir.
             */
            assertThat(successCount)
                    .isEqualTo(1);

            assertThat(failureCount)
                    .isEqualTo(1);

            /*
             * Uduzan transaction PostgreSQL partial
             * unique index səbəbindən reject olunmalıdır.
             */
            RaceResult failedResult =
                    results.stream()
                            .filter(result ->
                                    !result.success()
                            )
                            .findFirst()
                            .orElseThrow();

            assertThat(
                    failedResult.error()
            )
                    .isInstanceOf(
                            DataIntegrityViolationException.class
                    );

            /*
             * DB-də eyni vehicle üçün yalnız
             * bir IN_PROGRESS maintenance qalmalıdır.
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
             * İki maintenance-dən:
             *
             * biri IN_PROGRESS,
             * digəri isə rollback nəticəsində SCHEDULED
             * qalmalıdır.
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
        }
        finally {

            executor.shutdownNow();

            executor.awaitTermination(
                    5,
                    TimeUnit.SECONDS
            );
        }
    }

    private RaceResult moveToInProgress(
            Long maintenanceId,
            CountDownLatch readyLatch,
            CountDownLatch flushLatch
    ) {

        TransactionTemplate transaction =
                new TransactionTemplate(
                        transactionManager
                );

        try {

            transaction.executeWithoutResult(
                    transactionStatus -> {

                        MaintenanceRecord record =
                                maintenanceRepository
                                        .findById(
                                                maintenanceId
                                        )
                                        .orElseThrow();

                        record.setStatus(
                                MaintenanceStatus.IN_PROGRESS
                        );

                        /*
                         * Bu transaction artıq record-u
                         * dəyişməyə hazırdır.
                         */
                        readyLatch.countDown();

                        /*
                         * Hər iki transaction flush
                         * nöqtəsinə gəldiyini bildirir.
                         */
                        flushLatch.countDown();

                        try {

                            boolean released =
                                    flushLatch.await(
                                            10,
                                            TimeUnit.SECONDS
                                    );

                            if (!released) {

                                throw new IllegalStateException(
                                        "Concurrency barrier timed out"
                                );
                            }
                        }
                        catch (
                                InterruptedException exception
                        ) {

                            Thread.currentThread()
                                    .interrupt();

                            throw new IllegalStateException(
                                    "Concurrency test was interrupted",
                                    exception
                            );
                        }

                        /*
                         * Hər iki thread mümkün qədər
                         * eyni anda flush edir.
                         *
                         * PostgreSQL partial unique index:
                         *
                         * eyni vehicle üçün yalnız bir
                         * IN_PROGRESS row-a icazə verməlidir.
                         */
                        maintenanceRepository
                                .saveAndFlush(
                                        record
                                );
                    }
            );

            return RaceResult.successResult();
        }
        catch (
                RuntimeException exception
        ) {

            return RaceResult.failureResult(
                    exception
            );
        }
    }

    private Vehicle createVehicle() {

        Vehicle vehicle =
                new Vehicle();

        vehicle.setVin(
                "TESTVIN0000000001"
        );

        vehicle.setLicensePlate(
                "99-TC-001"
        );

        vehicle.setMake(
                "Test"
        );

        vehicle.setModel(
                "Concurrency"
        );

        /*
         * vehicles.manufacture_year NOT NULL-dır.
         */
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
                "Phase 17 concurrency test"
        );

        record.setServiceDate(
                LocalDate.now()
        );

        record.setNextServiceDate(
                LocalDate.now()
                        .plusMonths(6)
        );

        /*
         * setOdometer Long gözləyir.
         */
        record.setOdometer(
                10_000L
        );

        record.setCost(
                BigDecimal.valueOf(
                        100
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

    private record RaceResult(
            boolean success,
            RuntimeException error
    ) {

        private static RaceResult successResult() {

            return new RaceResult(
                    true,
                    null
            );
        }

        private static RaceResult failureResult(
                RuntimeException error
        ) {

            return new RaceResult(
                    false,
                    error
            );
        }
    }
}