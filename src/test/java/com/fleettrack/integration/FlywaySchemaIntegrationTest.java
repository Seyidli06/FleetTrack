package com.fleettrack.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FlywaySchemaIntegrationTest
        extends AbstractIntegrationTest {

    private static final int EXPECTED_MIGRATION_COUNT = 9;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldApplyAllFlywayMigrations() {

        Integer successfulMigrationCount =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM flyway_schema_history
                        WHERE success = TRUE
                        """,
                        Integer.class
                );

        String latestVersion =
                jdbcTemplate.queryForObject(
                        """
                        SELECT version
                        FROM flyway_schema_history
                        WHERE success = TRUE
                        ORDER BY installed_rank DESC
                        LIMIT 1
                        """,
                        String.class
                );

        assertThat(successfulMigrationCount)
                .isEqualTo(
                        EXPECTED_MIGRATION_COUNT
                );

        assertThat(latestVersion)
                .isEqualTo("9");
    }

    @Test
    void shouldHaveMaintenanceConcurrencySchema() {

        Boolean versionColumnExists =
                jdbcTemplate.queryForObject(
                        """
                        SELECT EXISTS (
                            SELECT 1
                            FROM information_schema.columns
                            WHERE table_schema = 'public'
                              AND table_name = 'maintenance_records'
                              AND column_name = 'version'
                              AND data_type = 'bigint'
                              AND is_nullable = 'NO'
                        )
                        """,
                        Boolean.class
                );

        String inProgressIndexDefinition =
                jdbcTemplate.queryForObject(
                        """
                        SELECT indexdef
                        FROM pg_indexes
                        WHERE schemaname = 'public'
                          AND tablename = 'maintenance_records'
                          AND indexname = 'uk_maintenance_in_progress_vehicle'
                        """,
                        String.class
                );

        assertThat(versionColumnExists)
                .isTrue();

        assertThat(inProgressIndexDefinition)
                .isNotNull()
                .containsIgnoringCase(
                        "UNIQUE INDEX"
                )
                .contains(
                        "vehicle_id"
                )
                .contains(
                        "IN_PROGRESS"
                );
    }

    @Test
    void shouldHaveProductionPerformanceAndIntegrityIndexes() {

        List<String> indexNames =
                jdbcTemplate.queryForList(
                        """
                        SELECT indexname
                        FROM pg_indexes
                        WHERE schemaname = 'public'
                        """,
                        String.class
                );

        assertThat(indexNames)
                .contains(
                        "idx_vehicles_created_at",
                        "idx_drivers_created_at",
                        "idx_maintenance_status_next_service_date",

                        "idx_drivers_first_name_trgm",
                        "idx_drivers_last_name_trgm",

                        "uk_vehicles_vin_ci",
                        "uk_vehicles_license_plate_ci",
                        "uk_drivers_license_number_ci",

                        "idx_vehicle_locations_vehicle_recorded_id"
                );

        assertThat(indexNames)
                .doesNotContain(
                        "idx_vehicle_locations_vehicle_recorded_at"
                );

        String locationIndexDefinition =
                jdbcTemplate.queryForObject(
                        """
                        SELECT indexdef
                        FROM pg_indexes
                        WHERE schemaname = 'public'
                          AND tablename = 'vehicle_locations'
                          AND indexname =
                              'idx_vehicle_locations_vehicle_recorded_id'
                        """,
                        String.class
                );

        assertThat(locationIndexDefinition)
                .isNotNull()
                .contains(
                        "vehicle_id"
                )
                .contains(
                        "recorded_at DESC"
                )
                .contains(
                        "id DESC"
                );

        Boolean pgTrgmInstalled =
                jdbcTemplate.queryForObject(
                        """
                        SELECT EXISTS (
                            SELECT 1
                            FROM pg_extension
                            WHERE extname = 'pg_trgm'
                        )
                        """,
                        Boolean.class
                );

        assertThat(pgTrgmInstalled)
                .isTrue();
    }
}