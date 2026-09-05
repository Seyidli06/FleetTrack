package com.fleettrack.assignment.repository;

import com.fleettrack.assignment.entity.VehicleAssignment;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VehicleAssignmentRepository
        extends JpaRepository<VehicleAssignment, Long> {

    boolean existsByVehicle_IdAndUnassignedAtIsNull(
            Long vehicleId
    );

    boolean existsByDriver_IdAndUnassignedAtIsNull(
            Long driverId
    );

    @EntityGraph(attributePaths = {
            "vehicle",
            "driver"
    })
    Optional<VehicleAssignment>
    findByVehicle_IdAndUnassignedAtIsNull(
            Long vehicleId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            "vehicle",
            "driver"
    })
    @Query("""
            select a
            from VehicleAssignment a
            where a.vehicle.id = :vehicleId
              and a.unassignedAt is null
            """)
    Optional<VehicleAssignment> findActiveForUpdate(
            @Param("vehicleId") Long vehicleId
    );

    @EntityGraph(attributePaths = {
            "vehicle",
            "driver"
    })
    Page<VehicleAssignment> findByVehicle_Id(
            Long vehicleId,
            Pageable pageable
    );
}