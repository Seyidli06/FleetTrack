CREATE INDEX idx_vehicle_locations_vehicle_recorded_id
    ON vehicle_locations (
                          vehicle_id,
                          recorded_at DESC,
                          id DESC
        );

DROP INDEX idx_vehicle_locations_vehicle_recorded_at;