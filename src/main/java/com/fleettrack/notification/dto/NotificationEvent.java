package com.fleettrack.notification.dto;

import com.fleettrack.notification.model.NotificationType;

import java.time.Instant;

public record NotificationEvent(

        String eventId,

        NotificationType type,

        Long vehicleId,

        Long referenceId,

        String title,

        String message,

        Instant occurredAt
) {
}