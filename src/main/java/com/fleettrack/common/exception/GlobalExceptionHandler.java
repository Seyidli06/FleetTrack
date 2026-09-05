package com.fleettrack.common.exception;

import com.fleettrack.common.error.ApiErrorCode;
import com.fleettrack.common.error.ApiProblemFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(
                    GlobalExceptionHandler.class
            );

    private final ApiProblemFactory problemFactory;

    public GlobalExceptionHandler(
            ApiProblemFactory problemFactory
    ) {
        this.problemFactory =
                problemFactory;
    }


    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResourceFound(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {
        ProblemDetail problem =
                problemFactory.create(
                        HttpStatus.NOT_FOUND,
                        "Resource not found",
                        "The requested resource was not found.",
                        ApiErrorCode.RESOURCE_NOT_FOUND,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(problem);
    }

    @ExceptionHandler(
            ResourceNotFoundException.class
    )
    public ResponseEntity<ProblemDetail>
    handleResourceNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {
        return response(
                problemFactory.create(
                        HttpStatus.NOT_FOUND,
                        "Resource not found",
                        exception.getMessage(),
                        ApiErrorCode.RESOURCE_NOT_FOUND,
                        request
                )
        );
    }

    @ExceptionHandler(
            DuplicateResourceException.class
    )
    public ResponseEntity<ProblemDetail>
    handleDuplicateResource(
            DuplicateResourceException exception,
            HttpServletRequest request
    ) {
        return response(
                problemFactory.create(
                        HttpStatus.CONFLICT,
                        "Duplicate resource",
                        exception.getMessage(),
                        ApiErrorCode.DUPLICATE_RESOURCE,
                        request
                )
        );
    }

    @ExceptionHandler(
            BusinessRuleViolationException.class
    )
    public ResponseEntity<ProblemDetail>
    handleBusinessRuleViolation(
            BusinessRuleViolationException exception,
            HttpServletRequest request
    ) {
        return response(
                problemFactory.create(
                        HttpStatus.CONFLICT,
                        "Business rule conflict",
                        exception.getMessage(),
                        ApiErrorCode.BUSINESS_RULE_CONFLICT,
                        request
                )
        );
    }

    @ExceptionHandler(
            StaleVersionException.class
    )
    public ResponseEntity<ProblemDetail>
    handleStaleVersion(
            StaleVersionException exception,
            HttpServletRequest request
    ) {
        return response(
                problemFactory.create(
                        HttpStatus.CONFLICT,
                        "Stale version",
                        exception.getMessage(),
                        ApiErrorCode.STALE_VERSION,
                        request
                )
        );
    }

    @ExceptionHandler(
            OptimisticLockingFailureException.class
    )
    public ResponseEntity<ProblemDetail>
    handleOptimisticLockingFailure(
            OptimisticLockingFailureException exception,
            HttpServletRequest request
    ) {
        log.warn(
                "Optimistic locking conflict for {}",
                request.getRequestURI()
        );

        return response(
                problemFactory.create(
                        HttpStatus.CONFLICT,
                        "Concurrent update conflict",
                        "The resource was changed by another request. "
                                + "Reload it and try again.",
                        ApiErrorCode.STALE_VERSION,
                        request
                )
        );
    }

    @ExceptionHandler(
            DataIntegrityViolationException.class
    )
    public ResponseEntity<ProblemDetail>
    handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        log.warn(
                "Database constraint violation for {}",
                request.getRequestURI()
        );

        /*
         * Database exception-un daxili mesajını
         * client-ə vermirik.
         */
        return response(
                problemFactory.create(
                        HttpStatus.CONFLICT,
                        "Data integrity conflict",
                        "The operation conflicts with an existing "
                                + "database constraint.",
                        ApiErrorCode.DATA_INTEGRITY_CONFLICT,
                        request
                )
        );
    }

    @ExceptionHandler(
            MethodArgumentNotValidException.class
    )
    public ResponseEntity<ProblemDetail>
    handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> errors =
                new LinkedHashMap<>();

        for (
                FieldError fieldError
                : exception
                .getBindingResult()
                .getFieldErrors()
        ) {
            errors.putIfAbsent(
                    fieldError.getField(),
                    fieldError.getDefaultMessage()
            );
        }

        ProblemDetail problem =
                problemFactory
                        .createValidationProblem(
                                "One or more request fields are invalid.",
                                errors,
                                request
                        );

        return response(problem);
    }

    @ExceptionHandler(
            MethodArgumentTypeMismatchException.class
    )
    public ResponseEntity<ProblemDetail>
    handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        String detail =
                "Invalid value for parameter '"
                        + exception.getName()
                        + "'.";

        return response(
                problemFactory.create(
                        HttpStatus.BAD_REQUEST,
                        "Invalid request parameter",
                        detail,
                        ApiErrorCode.BAD_REQUEST,
                        request
                )
        );
    }

    @ExceptionHandler(
            HttpMessageNotReadableException.class
    )
    public ResponseEntity<ProblemDetail>
    handleMalformedJson(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return response(
                problemFactory.create(
                        HttpStatus.BAD_REQUEST,
                        "Malformed request",
                        "The request body is missing or contains invalid JSON.",
                        ApiErrorCode.MALFORMED_REQUEST,
                        request
                )
        );
    }

    @ExceptionHandler(
            IllegalArgumentException.class
    )
    public ResponseEntity<ProblemDetail>
    handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        return response(
                problemFactory.create(
                        HttpStatus.BAD_REQUEST,
                        "Bad request",
                        exception.getMessage(),
                        ApiErrorCode.BAD_REQUEST,
                        request
                )
        );
    }

    /*
     * Login service daxilində AuthenticationManager
     * exception atarsa MVC daxilində olduğuna görə
     * bu handler onu tutur.
     */
    @ExceptionHandler(
            AuthenticationException.class
    )
    public ResponseEntity<ProblemDetail>
    handleAuthentication(
            AuthenticationException exception,
            HttpServletRequest request
    ) {
        return response(
                problemFactory.create(
                        HttpStatus.UNAUTHORIZED,
                        "Unauthorized",
                        "Invalid username or password.",
                        ApiErrorCode.UNAUTHORIZED,
                        request
                )
        );
    }

    @ExceptionHandler(
            Exception.class
    )
    public ResponseEntity<ProblemDetail>
    handleUnexpected(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error(
                "Unexpected error while processing {}",
                request.getRequestURI(),
                exception
        );

        /*
         * Stack trace və daxili exception message
         * client-ə çıxmır.
         */
        return response(
                problemFactory.create(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Internal server error",
                        "An unexpected error occurred.",
                        ApiErrorCode.INTERNAL_ERROR,
                        request
                )
        );
    }

    private ResponseEntity<ProblemDetail> response(
            ProblemDetail problem
    ) {
        return ResponseEntity
                .status(
                        problem.getStatus()
                )
                .body(problem);
    }
}