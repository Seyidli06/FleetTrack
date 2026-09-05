ALTER TABLE maintenance_records
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE UNIQUE INDEX uk_maintenance_in_progress_vehicle
    ON maintenance_records (vehicle_id)
    WHERE status = 'IN_PROGRESS';