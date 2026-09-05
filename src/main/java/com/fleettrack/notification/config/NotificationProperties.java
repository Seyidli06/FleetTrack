package com.fleettrack.notification.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(
        prefix = "app.notification"
)
@Validated
public record NotificationProperties(

        @NotBlank
        String channel
) {
}