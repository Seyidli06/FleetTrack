package com.fleettrack.ratelimit.service;

import com.fleettrack.ratelimit.dto.RateLimitDecision;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class RedisRateLimitService {

    private final StringRedisTemplate redisTemplate;

    private final RedisScript<String>
            rateLimitScript;

    public RedisRateLimitService(
            StringRedisTemplate redisTemplate,
            RedisScript<String> rateLimitScript
    ) {
        this.redisTemplate =
                redisTemplate;

        this.rateLimitScript =
                rateLimitScript;
    }

    public RateLimitDecision check(
            String key,
            int limit,
            Duration window
    ) {

        long windowSeconds =
                Math.max(
                        1,
                        window.toSeconds()
                );

        String result =
                redisTemplate.execute(
                        rateLimitScript,
                        List.of(key),
                        String.valueOf(
                                windowSeconds
                        )
                );

        if (result == null) {
            throw new IllegalStateException(
                    "Redis rate limit script returned no result"
            );
        }

        String[] parts =
                result.split(":");

        if (parts.length != 2) {
            throw new IllegalStateException(
                    "Unexpected Redis rate limit result: "
                            + result
            );
        }

        long current =
                Long.parseLong(
                        parts[0]
                );

        long ttl =
                Long.parseLong(
                        parts[1]
                );

        if (ttl <= 0) {
            ttl = windowSeconds;
        }

        boolean allowed =
                current <= limit;

        int remaining =
                (int) Math.max(
                        0,
                        limit - current
                );

        return new RateLimitDecision(
                allowed,
                limit,
                remaining,
                ttl
        );
    }
}