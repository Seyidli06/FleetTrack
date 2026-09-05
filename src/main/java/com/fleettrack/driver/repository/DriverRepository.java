package com.fleettrack.driver.repository;

import com.fleettrack.driver.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DriverRepository
        extends JpaRepository<Driver, Long>,
        JpaSpecificationExecutor<Driver> {

    boolean existsByLicenseNumberIgnoreCase(
            String licenseNumber
    );

    boolean existsByLicenseNumberIgnoreCaseAndIdNot(
            String licenseNumber,
            Long id
    );
}