CREATE INDEX idx_vehicles_status
    ON vehicles (status);

CREATE INDEX idx_vehicles_make_model
    ON vehicles (make, model);

CREATE INDEX idx_vehicles_manufacture_year
    ON vehicles (manufacture_year);

CREATE INDEX idx_drivers_status
    ON drivers (status);

CREATE INDEX idx_drivers_license_expiry_date
    ON drivers (license_expiry_date);

CREATE INDEX idx_vehicle_assignments_vehicle_id
    ON vehicle_assignments (vehicle_id);

CREATE INDEX idx_vehicle_assignments_driver_id
    ON vehicle_assignments (driver_id);

CREATE UNIQUE INDEX uk_vehicle_assignments_active_vehicle
    ON vehicle_assignments (vehicle_id)
    WHERE unassigned_at IS NULL;

CREATE UNIQUE INDEX uk_vehicle_assignments_active_driver
    ON vehicle_assignments (driver_id)
    WHERE unassigned_at IS NULL;

CREATE INDEX idx_maintenance_vehicle_service_date
    ON maintenance_records (vehicle_id, service_date DESC);

CREATE INDEX idx_maintenance_next_service_date
    ON maintenance_records (next_service_date)
    WHERE next_service_date IS NOT NULL;

CREATE INDEX idx_vehicle_locations_vehicle_recorded_at
    ON vehicle_locations (vehicle_id, recorded_at DESC);