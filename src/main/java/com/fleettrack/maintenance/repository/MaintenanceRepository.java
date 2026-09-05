package com.fleettrack.maintenance.repository;

import com.fleettrack.maintenance.dto.MaintenanceReminderCandidate;
import com.fleettrack.maintenance.entity.MaintenanceRecord;
import com.fleettrack.maintenance.entity.MaintenanceStatus;
import com.fleettrack.report.dto.MaintenanceReportRow;
import com.fleettrack.vehicle.entity.VehicleStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MaintenanceRepository
        extends JpaRepository<MaintenanceRecord, Long>,
        JpaSpecificationExecutor<MaintenanceRecord> {

    boolean existsByVehicle_IdAndStatus(
            Long vehicleId,
            MaintenanceStatus status
    );

    @EntityGraph(attributePaths = "vehicle")
    @Query("""
            select m
            from MaintenanceRecord m
            where m.id = :id
            """)
    Optional<MaintenanceRecord> findDetailedById(
            @Param("id")
            Long id
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "vehicle")
    @Query("""
            select m
            from MaintenanceRecord m
            where m.id = :id
            """)
    Optional<MaintenanceRecord> findForUpdate(
            @Param("id")
            Long id
    );

    @Query("""
            select new com.fleettrack.maintenance.dto.MaintenanceReminderCandidate(
                m.id,
                v.id,
                v.vin,
                v.licensePlate,
                m.maintenanceType,
                m.nextServiceDate
            )
            from MaintenanceRecord m
            join m.vehicle v
            where m.status = :maintenanceStatus
              and m.nextServiceDate is not null
              and m.nextServiceDate >= :dateFrom
              and m.nextServiceDate <= :dateTo
              and v.status <> :retiredStatus
            order by m.nextServiceDate asc
            """)
    List<MaintenanceReminderCandidate>
    findReminderCandidates(
            @Param("maintenanceStatus")
            MaintenanceStatus maintenanceStatus,

            @Param("retiredStatus")
            VehicleStatus retiredStatus,

            @Param("dateFrom")
            LocalDate dateFrom,

            @Param("dateTo")
            LocalDate dateTo
    );

    @Query("""
            select new com.fleettrack.report.dto.MaintenanceReportRow(
                m.id,
                m.maintenanceType,
                m.serviceDate,
                m.nextServiceDate,
                m.odometer,
                m.cost,
                m.status
            )
            from MaintenanceRecord m
            where m.vehicle.id = :vehicleId
            order by m.serviceDate desc, m.id desc
            """)
    List<MaintenanceReportRow> findReportRowsByVehicleId(
            @Param("vehicleId")
            Long vehicleId
    );
}