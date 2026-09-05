package com.fleettrack.ratelimit;

import com.fleettrack.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@TestPropertySource(
        properties = {
                "app.rate-limit.enabled=true",
                "app.rate-limit.login.requests=3",
                "app.rate-limit.login.window=1m"
        }
)
class RateLimitHttpIntegrationTest
        extends AbstractIntegrationTest {

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {

        clearRateLimitKeys();

        mockMvc =
                MockMvcBuilders
                        .webAppContextSetup(
                                applicationContext
                        )
                        .apply(
                                springSecurity()
                        )
                        .build();
    }

    @Test
    void shouldReturn429AfterLoginLimitIsExceeded()
            throws Exception {


        String loginBody =
                """
                {
                  "username": "phase17-user",
                  "password": "wrong-password"
                }
                """;


        mockMvc.perform(
                        post(
                                "/api/v1/auth/login"
                        )
                                .contentType(
                                        "application/json"
                                )
                                .content(
                                        loginBody
                                )
                )
                .andExpect(
                        status().isUnauthorized()
                )
                .andExpect(
                        header()
                                .string(
                                        "X-RateLimit-Limit",
                                        "3"
                                )
                )
                .andExpect(
                        header()
                                .string(
                                        "X-RateLimit-Remaining",
                                        "2"
                                )
                );


        mockMvc.perform(
                        post(
                                "/api/v1/auth/login"
                        )
                                .contentType(
                                        "application/json"
                                )
                                .content(
                                        loginBody
                                )
                )
                .andExpect(
                        status().isUnauthorized()
                )
                .andExpect(
                        header()
                                .string(
                                        "X-RateLimit-Limit",
                                        "3"
                                )
                )
                .andExpect(
                        header()
                                .string(
                                        "X-RateLimit-Remaining",
                                        "1"
                                )
                );


        mockMvc.perform(
                        post(
                                "/api/v1/auth/login"
                        )
                                .contentType(
                                        "application/json"
                                )
                                .content(
                                        loginBody
                                )
                )
                .andExpect(
                        status().isUnauthorized()
                )
                .andExpect(
                        header()
                                .string(
                                        "X-RateLimit-Limit",
                                        "3"
                                )
                )
                .andExpect(
                        header()
                                .string(
                                        "X-RateLimit-Remaining",
                                        "0"
                                )
                );



        mockMvc.perform(
                        post(
                                "/api/v1/auth/login"
                        )
                                .contentType(
                                        "application/json"
                                )
                                .content(
                                        loginBody
                                )
                )
                .andExpect(
                        status().isTooManyRequests()
                )
                .andExpect(
                        content()
                                .contentTypeCompatibleWith(
                                        "application/problem+json"
                                )
                )
                .andExpect(
                        header()
                                .string(
                                        "X-RateLimit-Limit",
                                        "3"
                                )
                )
                .andExpect(
                        header()
                                .string(
                                        "X-RateLimit-Remaining",
                                        "0"
                                )
                )
                .andExpect(
                        header()
                                .exists(
                                        "X-RateLimit-Reset"
                                )
                )
                .andExpect(
                        header()
                                .exists(
                                        "Retry-After"
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.status"
                        )
                                .value(
                                        429
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.title"
                        )
                                .value(
                                        "Too many requests"
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.code"
                        )
                                .value(
                                        "RATE_LIMIT_EXCEEDED"
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.instance"
                        )
                                .value(
                                        "/api/v1/auth/login"
                                )
                );


        Set<String> keys =
                redisTemplate.keys(
                        "rate-limit:login:*"
                );

        assertThat(keys)
                .isNotNull();

        assertThat(keys)
                .hasSize(1);

        String key =
                keys.iterator()
                        .next();

        String counter =
                redisTemplate
                        .opsForValue()
                        .get(
                                key
                        );


        assertThat(counter)
                .isEqualTo(
                        "4"
                );

        Long ttl =
                redisTemplate.getExpire(
                        key,
                        TimeUnit.SECONDS
                );

        assertThat(ttl)
                .isNotNull();

        assertThat(ttl)
                .isBetween(
                        1L,
                        60L
                );
    }

    private void clearRateLimitKeys() {

        Set<String> keys =
                redisTemplate.keys(
                        "rate-limit:*"
                );

        if (keys != null
                && !keys.isEmpty()) {

            redisTemplate.delete(
                    keys
            );
        }
    }
}