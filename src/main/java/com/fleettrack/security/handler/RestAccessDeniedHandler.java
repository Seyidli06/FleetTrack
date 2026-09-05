package com.fleettrack.security.handler;

import com.fleettrack.common.error.ApiErrorCode;
import com.fleettrack.common.error.ApiProblemFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
public class RestAccessDeniedHandler
        implements AccessDeniedHandler {

    private final ApiProblemFactory problemFactory;
    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(
            ApiProblemFactory problemFactory,
            ObjectMapper objectMapper
    ) {
        this.problemFactory =
                problemFactory;

        this.objectMapper =
                objectMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {

        ProblemDetail problem =
                problemFactory.create(
                        HttpStatus.FORBIDDEN,
                        "Forbidden",
                        "You do not have permission to access this resource.",
                        ApiErrorCode.FORBIDDEN,
                        request
                );

        response.setStatus(
                HttpServletResponse.SC_FORBIDDEN
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