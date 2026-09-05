package com.fleettrack.security.websocket;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class StompAuthorizationInterceptor
        implements ChannelInterceptor {

    private static final Pattern VEHICLE_LOCATION_TOPIC =
            Pattern.compile(
                    "^/topic/vehicles/\\d+/location$"
            );

    private static final Set<String> ALLOWED_ROLES =
            Set.of(
                    "ROLE_ADMIN",
                    "ROLE_FLEET_MANAGER"
            );

    @Override
    public Message<?> preSend(
            Message<?> message,
            MessageChannel channel
    ) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(
                        message,
                        StompHeaderAccessor.class
                );

        if (accessor == null) {
            return message;
        }

        StompCommand command =
                accessor.getCommand();

        if (command == null) {
            return message;
        }

        if (command == StompCommand.SUBSCRIBE) {
            authorizeSubscription(accessor);
        }

        if (command == StompCommand.SEND) {
            throw new AccessDeniedException(
                    "Client STOMP SEND is not allowed"
            );
        }

        return message;
    }

    private void authorizeSubscription(
            StompHeaderAccessor accessor
    ) {

        Principal principal =
                accessor.getUser();

        if (!(principal
                instanceof Authentication authentication)) {

            throw new AccessDeniedException(
                    "Authentication is required"
            );
        }

        if (!authentication.isAuthenticated()) {

            throw new AccessDeniedException(
                    "Authentication is required"
            );
        }

        boolean allowedRole =
                authentication
                        .getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                ALLOWED_ROLES.contains(
                                        authority.getAuthority()
                                )
                        );

        if (!allowedRole) {

            throw new AccessDeniedException(
                    "Insufficient WebSocket permissions"
            );
        }

        String destination =
                accessor.getDestination();

        if (destination == null
                || !VEHICLE_LOCATION_TOPIC
                .matcher(destination)
                .matches()) {

            throw new AccessDeniedException(
                    "Subscription destination is not allowed"
            );
        }
    }
}