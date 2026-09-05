package com.fleettrack.security.websocket;

import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JwtStompAuthenticationInterceptor
        implements ChannelInterceptor {

    private static final String BEARER_PREFIX =
            "Bearer ";

    private final JwtDecoder jwtDecoder;

    public JwtStompAuthenticationInterceptor(
            JwtDecoder jwtDecoder
    ) {
        this.jwtDecoder = jwtDecoder;
    }

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

        if (StompCommand.CONNECT.equals(
                accessor.getCommand()
        )) {
            authenticate(accessor);
        }

        return message;
    }

    private void authenticate(
            StompHeaderAccessor accessor
    ) {

        String authorization =
                accessor.getFirstNativeHeader(
                        HttpHeaders.AUTHORIZATION
                );

        if (authorization == null
                || !authorization.startsWith(
                BEARER_PREFIX
        )) {

            throw new BadCredentialsException(
                    "Missing WebSocket Bearer token"
            );
        }

        String token =
                authorization.substring(
                        BEARER_PREFIX.length()
                );

        try {

            Jwt jwt =
                    jwtDecoder.decode(token);

            List<String> roles =
                    jwt.getClaimAsStringList(
                            "roles"
                    );

            var authorities =
                    roles == null
                            ? List
                            .<SimpleGrantedAuthority>of()
                            : roles.stream()
                            .map(role ->
                                 new SimpleGrantedAuthority(
                                         "ROLE_" + role
                                 )
                            )
                            .toList();

            JwtAuthenticationToken authentication =
                    new JwtAuthenticationToken(
                            jwt,
                            authorities,
                            jwt.getSubject()
                    );

            /*
             * Ən vacib hissə:
             * Principal WebSocket session-a bağlanır.
             */
            accessor.setUser(authentication);

        } catch (JwtException exception) {

            throw new BadCredentialsException(
                    "Invalid or expired WebSocket token",
                    exception
            );
        }
    }
}