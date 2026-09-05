CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_drivers_first_name_trgm
    ON drivers
    USING gin (
    lower(first_name) gin_trgm_ops
    );

CREATE INDEX idx_drivers_last_name_trgm
    ON drivers
    USING gin (
    lower(last_name) gin_trgm_ops
    );