package com.fleettrack.auth.service;

import com.fleettrack.auth.dto.LoginRequest;
import com.fleettrack.auth.dto.TokenResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final String ROLE_PREFIX = "ROLE_";

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public TokenResponse login(LoginRequest request) {

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.username(),
                                request.password()
                        )
                );

        Set<String> roles = authentication
                .getAuthorities()
                .stream()
                .map(authority -> authority.getAuthority())
                .filter(authority ->
                        authority.startsWith(ROLE_PREFIX)
                )
                .map(authority ->
                        authority.substring(
                                ROLE_PREFIX.length()
                        )
                )
                .collect(Collectors.toSet());

        String accessToken =
                jwtService.generateToken(
                        authentication.getName(),
                        roles
                );

        return new TokenResponse(
                accessToken,
                "Bearer",
                jwtService.getExpiresInSeconds()
        );
    }
}