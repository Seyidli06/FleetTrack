package com.fleettrack.location.repository;

import com.fleettrack.location.entity.VehicleLocation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface VehicleLocationRepository
        extends JpaRepository<VehicleLocation, Long> {

    @EntityGraph(attributePaths = "vehicle")
    Optional<VehicleLocation>
    findFirstByVehicle_IdOrderByRecordedAtDescIdDesc(
            Long vehicleId
    );

    @EntityGraph(attributePaths = "vehicle")
    Slice<VehicleLocation>
    findByVehicle_IdOrderByRecordedAtDescIdDesc(
            Long vehicleId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "vehicle")
    @Query("""
            select location
            from VehicleLocation location
            where location.vehicle.id = :vehicleId
              and (
                    location.recordedAt,
                    location.id
                  ) < (
                    :cursorRecordedAt,
                    :cursorId
                  )
            order by
                location.recordedAt desc,
                location.id desc
            """)
    Slice<VehicleLocation> findAfterCursor(
            @Param("vehicleId")
            Long vehicleId,

            @Param("cursorRecordedAt")
            Instant cursorRecordedAt,

            @Param("cursorId")
            Long cursorId,

            Pageable pageable
    );
}