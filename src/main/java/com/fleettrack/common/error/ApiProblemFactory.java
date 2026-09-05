package com.fleettrack.common.error;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

@Component
public class ApiProblemFactory {

    public ProblemDetail create(
            HttpStatus status,
            String title,
            String detail,
            ApiErrorCode code,
            HttpServletRequest request
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        status,
                        detail
                );

        problem.setTitle(title);

        problem.setInstance(
                URI.create(
                        request.getRequestURI()
                )
        );

        problem.setProperty(
                "code",
                code.name()
        );

        problem.setProperty(
                "timestamp",
                Instant.now()
        );

        return problem;
    }

    public ProblemDetail createValidationProblem(
            String detail,
            Map<String, String> errors,
            HttpServletRequest request
    ) {
        ProblemDetail problem =
                create(
                        HttpStatus.BAD_REQUEST,
                        "Validation failed",
                        detail,
                        ApiErrorCode.VALIDATION_ERROR,
                        request
                );

        problem.setProperty(
                "errors",
                errors
        );

        return problem;
    }
}