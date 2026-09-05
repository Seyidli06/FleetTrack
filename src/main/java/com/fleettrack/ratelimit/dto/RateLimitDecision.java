package com.fleettrack.ratelimit.dto;

public record RateLimitDecision(

        boolean allowed,

        int limit,

        int remaining,

        long retryAfterSeconds
) {
}