package com.fleettrack.integration;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
class FleetTrackContextIntegrationTest
        extends AbstractIntegrationTest {

    @Test
    void contextLoadsWithPostgresAndRedisContainers() {

        assertTrue(
                POSTGRES.isRunning()
        );

        assertTrue(
                REDIS.isRunning()
        );
    }
}