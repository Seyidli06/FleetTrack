package com.fleettrack.security.handler;

import com.fleettrack.common.error.ApiErrorCode;
import com.fleettrack.common.error.ApiProblemFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
public class RestAuthenticationEntryPoint
        implements AuthenticationEntryPoint {

    private final ApiProblemFactory problemFactory;
    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(
            ApiProblemFactory problemFactory,
            ObjectMapper objectMapper
    ) {
        this.problemFactory =
                problemFactory;

        this.objectMapper =
                objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {

        ProblemDetail problem =
                problemFactory.create(
                        org.springframework.http.HttpStatus.UNAUTHORIZED,
                        "Unauthorized",
                        "Authentication is required to access this resource.",
                        ApiErrorCode.UNAUTHORIZED,
                        request
                );

        response.setStatus(
                HttpServletResponse.SC_UNAUTHORIZED
        );

        response.setContentType(
                MediaType.APPLICATION_PROBLEM_JSON_VALUE
        );

        objectMapper.writeValue(
                response.getOutputStream(),
                problem
        );
    }
}