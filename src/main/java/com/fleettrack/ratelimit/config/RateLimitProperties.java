package com.fleettrack.ratelimit.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(
        prefix = "app.rate-limit"
)
@Validated
public record RateLimitProperties(

        boolean enabled,

        @Valid
        @NotNull
        Policy login,

        @Valid
        @NotNull
        Policy api,

        @Valid
        @NotNull
        Policy reports
) {

    public record Policy(

            @Min(1)
            int requests,

            @NotNull
            Duration window
    ) {
    }
}