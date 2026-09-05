CREATE INDEX idx_vehicles_created_at
    ON vehicles (created_at DESC);

CREATE INDEX idx_drivers_created_at
    ON drivers (created_at DESC);

CREATE INDEX idx_maintenance_status_next_service_date
    ON maintenance_records (
                            status,
                            next_service_date
        )
    WHERE next_service_date IS NOT NULL;