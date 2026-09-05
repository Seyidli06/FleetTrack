package com.fleettrack.ratelimit;

import com.fleettrack.integration.AbstractIntegrationTest;
import com.fleettrack.ratelimit.service.RedisRateLimitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.rate-limit.enabled=true"
})
@Import(
        RateLimitRedisOutageIntegrationTest
                .FailingRateLimitConfiguration.class
)
class RateLimitRedisOutageIntegrationTest
        extends AbstractIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc() {

        return MockMvcBuilders
                .webAppContextSetup(context)
                .apply(
                        SecurityMockMvcConfigurers
                                .springSecurity()
                )
                .build();
    }

    @Test
    void loginShouldFailClosedWhenRateLimiterIsUnavailable()
            throws Exception {

        mockMvc()
                .perform(
                        post("/api/v1/auth/login")
                                .contentType("application/json")
                                .content("""
                                        {
                                          "username": "phase18-test",
                                          "password": "wrong-password"
                                        }
                                        """)
                )
                .andExpect(
                        status()
                                .isServiceUnavailable()
                )
                .andExpect(
                        content()
                                .contentTypeCompatibleWith(
                                        MediaType.APPLICATION_PROBLEM_JSON
                                )
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(503)
                )
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Service temporarily unavailable"
                                )
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "RATE_LIMIT_UNAVAILABLE"
                                )
                )
                .andExpect(
                        jsonPath("$.instance")
                                .value(
                                        "/api/v1/auth/login"
                                )
                );
    }

    @Test
    void protectedApiShouldFailOpenWhenRateLimiterIsUnavailable()
            throws Exception {

        /*
         * Rate limiter exception-a baxmayaraq
         * request security chain-də davam etməlidir.
         *
         * Bearer token olmadığı üçün son nəticə 401-dir.
         * Əsas invariant:
         *
         * Redis outage normal API-ni 503 etməməlidir.
         */
        mockMvc()
                .perform(
                        get("/api/v1/vehicles")
                )
                .andExpect(
                        status()
                                .isUnauthorized()
                );
    }

    @TestConfiguration
    static class FailingRateLimitConfiguration {

        @Bean
        @Primary
        RedisRateLimitService failingRateLimitService() {

            RedisRateLimitService service =
                    mock(
                            RedisRateLimitService.class
                    );

            when(
                    service.check(
                            any(),
                            anyInt(),
                            any()
                    )
            )
                    .thenThrow(
                            new IllegalStateException(
                                    "Simulated Redis outage"
                            )
                    );

            return service;
        }
    }
}