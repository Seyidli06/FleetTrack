ALTER TABLE vehicles
DROP CONSTRAINT uk_vehicles_vin;

ALTER TABLE vehicles
DROP CONSTRAINT uk_vehicles_license_plate;

ALTER TABLE drivers
DROP CONSTRAINT uk_drivers_license_number;


CREATE UNIQUE INDEX uk_vehicles_vin_ci
    ON vehicles (UPPER(vin));

CREATE UNIQUE INDEX uk_vehicles_license_plate_ci
    ON vehicles (UPPER(license_plate));

CREATE UNIQUE INDEX uk_drivers_license_number_ci
    ON drivers (UPPER(license_number));