package com.fleettrack.ratelimit.filter;

import com.fleettrack.common.error.ApiErrorCode;
import com.fleettrack.common.error.ApiProblemFactory;
import com.fleettrack.ratelimit.config.RateLimitProperties;
import com.fleettrack.ratelimit.dto.RateLimitDecision;
import com.fleettrack.ratelimit.service.RedisRateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class RateLimitFilter
        extends OncePerRequestFilter {

    private static final Logger log =
            LoggerFactory.getLogger(
                    RateLimitFilter.class
            );

    private static final String LOGIN_PATH =
            "/api/v1/auth/login";

    private static final String REPORT_PATH =
            "/api/v1/reports/";

    private final RedisRateLimitService
            rateLimitService;

    private final RateLimitProperties
            properties;

    private final ApiProblemFactory
            problemFactory;

    private final ObjectMapper
            objectMapper;

    public RateLimitFilter(
            RedisRateLimitService rateLimitService,
            RateLimitProperties properties,
            ApiProblemFactory problemFactory,
            ObjectMapper objectMapper
    ) {
        this.rateLimitService =
                rateLimitService;

        this.properties =
                properties;

        this.problemFactory =
                problemFactory;

        this.objectMapper =
                objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request
    ) {

        if (
                HttpMethod.OPTIONS.matches(
                        request.getMethod()
                )
        ) {
            return true;
        }

        return !request
                .getRequestURI()
                .startsWith(
                        "/api/v1/"
                );
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (!properties.enabled()) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }

        RateLimitTarget target =
                resolveTarget(
                        request
                );

        RateLimitDecision decision;

        try {

            decision =
                    rateLimitService.check(
                            target.redisKey(),
                            target.policy().requests(),
                            target.policy().window()
                    );

        } catch (RuntimeException exception) {

            if (LOGIN_PATH.equals(
                    request.getRequestURI()
            )) {

                log.error(
                        "Rate limiter unavailable for login; rejecting request",
                        exception
                );

                rejectRateLimitUnavailable(
                        request,
                        response
                );

                return;
            }

            /*
             * Authenticated API trafikində availability-first.
             *
             * Redis müvəqqəti əlçatan deyilsə,
             * rate limiter request-i bloklamır.
             */
            log.error(
                    "Rate limiter unavailable for path={}; allowing request",
                    request.getRequestURI(),
                    exception
            );

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }

        addRateLimitHeaders(
                response,
                decision
        );

        if (decision.allowed()) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }

        rejectRequest(
                request,
                response,
                decision
        );
    }

    private RateLimitTarget resolveTarget(
            HttpServletRequest request
    ) {

        String uri =
                request.getRequestURI();

        if (LOGIN_PATH.equals(uri)) {

            String identity =
                    "ip:"
                            + request.getRemoteAddr();

            return new RateLimitTarget(
                    "rate-limit:login:"
                            + hash(identity),
                    properties.login()
            );
        }

        String identity =
                resolveAuthenticatedIdentity(
                        request
                );

        if (
                uri.startsWith(
                        REPORT_PATH
                )
        ) {
            return new RateLimitTarget(
                    "rate-limit:report:"
                            + hash(identity),
                    properties.reports()
            );
        }

        return new RateLimitTarget(
                "rate-limit:api:"
                        + hash(identity),
                properties.api()
        );
    }

    private String resolveAuthenticatedIdentity(
            HttpServletRequest request
    ) {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (
                authentication != null
                        && authentication.isAuthenticated()
                        && !(authentication
                        instanceof AnonymousAuthenticationToken)
        ) {
            return "user:"
                    + authentication.getName();
        }

        /*
         * Unauthenticated protected request.
         *
         * Authorization filter sonradan 401 verəcək,
         * amma abuse protection üçün IP bucket istifadə olunur.
         */
        return "ip:"
                + request.getRemoteAddr();
    }

    private void addRateLimitHeaders(
            HttpServletResponse response,
            RateLimitDecision decision
    ) {

        response.setHeader(
                "X-RateLimit-Limit",
                String.valueOf(
                        decision.limit()
                )
        );

        response.setHeader(
                "X-RateLimit-Remaining",
                String.valueOf(
                        decision.remaining()
                )
        );

        response.setHeader(
                "X-RateLimit-Reset",
                String.valueOf(
                        decision.retryAfterSeconds()
                )
        );
    }

    private void rejectRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            RateLimitDecision decision
    ) throws IOException {

        ProblemDetail problem =
                problemFactory.create(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Too many requests",
                        "Rate limit exceeded. Retry after "
                                + decision.retryAfterSeconds()
                                + " second(s).",
                        ApiErrorCode.RATE_LIMIT_EXCEEDED,
                        request
                );

        response.setStatus(
                HttpStatus.TOO_MANY_REQUESTS.value()
        );

        response.setContentType(
                MediaType.APPLICATION_PROBLEM_JSON_VALUE
        );

        response.setCharacterEncoding(
                StandardCharsets.UTF_8.name()
        );

        response.setHeader(
                "Retry-After",
                String.valueOf(
                        decision.retryAfterSeconds()
                )
        );

        objectMapper.writeValue(
                response.getOutputStream(),
                problem
        );
    }

    private void rejectRateLimitUnavailable(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        ProblemDetail problem =
                problemFactory.create(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Service temporarily unavailable",
                        "Login is temporarily unavailable. Please try again later.",
                        ApiErrorCode.RATE_LIMIT_UNAVAILABLE,
                        request
                );

        response.setStatus(
                HttpStatus.SERVICE_UNAVAILABLE.value()
        );

        response.setContentType(
                MediaType.APPLICATION_PROBLEM_JSON_VALUE
        );

        response.setCharacterEncoding(
                StandardCharsets.UTF_8.name()
        );

        objectMapper.writeValue(
                response.getOutputStream(),
                problem
        );
    }

    private String hash(
            String value
    ) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] hashed =
                    digest.digest(
                            value.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            /*
             * Redis key-də username və IP-ni
             * plain-text saxlamırıq.
             */
            return HexFormat
                    .of()
                    .formatHex(hashed)
                    .substring(
                            0,
                            32
                    );

        } catch (
                NoSuchAlgorithmException exception
        ) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is unavailable",
                    exception
            );
        }
    }

    private record RateLimitTarget(

            String redisKey,

            RateLimitProperties.Policy policy
    ) {
    }
}