package com.fleettrack.config.websocket;

import com.fleettrack.security.websocket.JwtStompAuthenticationInterceptor;
import com.fleettrack.security.websocket.StompAuthorizationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig
        implements WebSocketMessageBrokerConfigurer {

    private final JwtStompAuthenticationInterceptor
            authenticationInterceptor;

    private final StompAuthorizationInterceptor
            authorizationInterceptor;

    public WebSocketConfig(
            JwtStompAuthenticationInterceptor authenticationInterceptor,
            StompAuthorizationInterceptor authorizationInterceptor
    ) {
        this.authenticationInterceptor =
                authenticationInterceptor;

        this.authorizationInterceptor =
                authorizationInterceptor;
    }

    @Override
    public void configureMessageBroker(
            MessageBrokerRegistry registry
    ) {
        registry.enableSimpleBroker(
                "/topic"
        );

        registry.setApplicationDestinationPrefixes(
                "/app"
        );
    }

    @Override
    public void registerStompEndpoints(
            StompEndpointRegistry registry
    ) {
        registry
                .addEndpoint("/ws")
                .setAllowedOriginPatterns(
                        "http://localhost:*",
                        "http://127.0.0.1:*"
                );
    }

    @Override
    public void configureClientInboundChannel(
            ChannelRegistration registration
    ) {
        registration.interceptors(
                authenticationInterceptor,
                authorizationInterceptor
        );
    }
}