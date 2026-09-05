package com.fleettrack.ratelimit;

import com.fleettrack.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
class RedisRateLimitLuaIntegrationTest
        extends AbstractIntegrationTest {

    private static final String TEST_KEY =
            "rate-limit:test:phase17";

    private static final long WINDOW_SECONDS =
            60L;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedisScript<String> rateLimitScript;

    @AfterEach
    void cleanup() {

        redisTemplate.delete(
                TEST_KEY
        );
    }

    @Test
    void shouldIncrementCounterAndKeepExpirationWindow() {

        RateLimitScriptResult first =
                executeScript();

        RateLimitScriptResult second =
                executeScript();

        RateLimitScriptResult third =
                executeScript();

        assertThat(first.current())
                .isEqualTo(1L);

        assertThat(second.current())
                .isEqualTo(2L);

        assertThat(third.current())
                .isEqualTo(3L);


        assertThat(first.ttl())
                .isBetween(
                        1L,
                        WINDOW_SECONDS
                );


        assertThat(second.ttl())
                .isLessThanOrEqualTo(
                        first.ttl()
                );

        assertThat(third.ttl())
                .isLessThanOrEqualTo(
                        second.ttl()
                );

        String redisCounter =
                redisTemplate.opsForValue()
                        .get(
                                TEST_KEY
                        );

        assertThat(redisCounter)
                .isEqualTo(
                        "3"
                );

        Long redisTtl =
                redisTemplate.getExpire(
                        TEST_KEY,
                        TimeUnit.SECONDS
                );

        assertThat(redisTtl)
                .isNotNull();

        assertThat(redisTtl)
                .isBetween(
                        1L,
                        WINDOW_SECONDS
                );
    }

    @Test
    void shouldIncrementAtomicallyWhenManyRequestsRace()
            throws Exception {

        int requestCount =
                20;

        ExecutorService executor =
                Executors.newFixedThreadPool(
                        requestCount
                );

        CountDownLatch readyLatch =
                new CountDownLatch(
                        requestCount
                );

        CountDownLatch startLatch =
                new CountDownLatch(
                        1
                );

        try {

            List<Future<RateLimitScriptResult>> futures =
                    new ArrayList<>();

            for (
                    int i = 0;
                    i < requestCount;
                    i++
            ) {

                futures.add(
                        executor.submit(() -> {

                            readyLatch.countDown();

                            boolean started =
                                    startLatch.await(
                                            10,
                                            TimeUnit.SECONDS
                                    );

                            if (!started) {

                                throw new IllegalStateException(
                                        "Redis concurrency start barrier timed out"
                                );
                            }

                            return executeScript();
                        })
                );
            }

            boolean allReady =
                    readyLatch.await(
                            10,
                            TimeUnit.SECONDS
                    );

            assertThat(allReady)
                    .as(
                            "All Redis workers should be ready"
                    )
                    .isTrue();


            startLatch.countDown();

            List<Long> counters =
                    new ArrayList<>();

            for (
                    Future<RateLimitScriptResult> future
                    : futures
            ) {

                RateLimitScriptResult result =
                        future.get(
                                15,
                                TimeUnit.SECONDS
                        );

                counters.add(
                        result.current()
                );

                assertThat(result.ttl())
                        .isBetween(
                                1L,
                                WINDOW_SECONDS
                        );
            }


            assertThat(counters)
                    .hasSize(
                            requestCount
                    );

            assertThat(counters)
                    .doesNotHaveDuplicates();

            Collections.sort(
                    counters
            );


            for (
                    int i = 0;
                    i < requestCount;
                    i++
            ) {

                assertThat(
                        counters.get(i)
                )
                        .isEqualTo(
                                i + 1L
                        );
            }

            String finalCounter =
                    redisTemplate
                            .opsForValue()
                            .get(
                                    TEST_KEY
                            );

            assertThat(finalCounter)
                    .isEqualTo(
                            String.valueOf(
                                    requestCount
                            )
                    );

            Long ttl =
                    redisTemplate.getExpire(
                            TEST_KEY,
                            TimeUnit.SECONDS
                    );

            assertThat(ttl)
                    .isNotNull();

            assertThat(ttl)
                    .isBetween(
                            1L,
                            WINDOW_SECONDS
                    );
        }
        finally {

            startLatch.countDown();

            executor.shutdownNow();

            executor.awaitTermination(
                    5,
                    TimeUnit.SECONDS
            );
        }
    }

    private RateLimitScriptResult executeScript() {

        String result =
                redisTemplate.execute(
                        rateLimitScript,
                        List.of(
                                TEST_KEY
                        ),
                        String.valueOf(
                                WINDOW_SECONDS
                        )
                );

        assertThat(result)
                .isNotNull();

        String[] parts =
                result.split(
                        ":"
                );

        assertThat(parts)
                .hasSize(
                        2
                );

        long current =
                Long.parseLong(
                        parts[0]
                );

        long ttl =
                Long.parseLong(
                        parts[1]
                );

        return new RateLimitScriptResult(
                current,
                ttl
        );
    }

    private record RateLimitScriptResult(
            long current,
            long ttl
    ) {
    }
}