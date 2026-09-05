package com.fleettrack.vehicle.repository;

import com.fleettrack.vehicle.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VehicleRepository
        extends JpaRepository<Vehicle, Long>,
        JpaSpecificationExecutor<Vehicle> {

    boolean existsByVinIgnoreCase(String vin);

    boolean existsByLicensePlateIgnoreCase(
            String licensePlate
    );

    boolean existsByVinIgnoreCaseAndIdNot(
            String vin,
            Long id
    );

    boolean existsByLicensePlateIgnoreCaseAndIdNot(
            String licensePlate,
            Long id
    );
}