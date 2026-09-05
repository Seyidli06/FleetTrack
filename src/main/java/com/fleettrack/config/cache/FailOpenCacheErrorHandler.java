package com.fleettrack.config.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

public class FailOpenCacheErrorHandler
        implements CacheErrorHandler {

    private static final Logger log =
            LoggerFactory.getLogger(
                    FailOpenCacheErrorHandler.class
            );

    @Override
    public void handleCacheGetError(
            RuntimeException exception,
            Cache cache,
            Object key
    ) {
        logFailure(
                "GET",
                cache,
                exception
        );
    }

    @Override
    public void handleCachePutError(
            RuntimeException exception,
            Cache cache,
            Object key,
            Object value
    ) {
        logFailure(
                "PUT",
                cache,
                exception
        );
    }

    @Override
    public void handleCacheEvictError(
            RuntimeException exception,
            Cache cache,
            Object key
    ) {
        logFailure(
                "EVICT",
                cache,
                exception
        );
    }

    @Override
    public void handleCacheClearError(
            RuntimeException exception,
            Cache cache
    ) {
        logFailure(
                "CLEAR",
                cache,
                exception
        );
    }

    private void logFailure(
            String operation,
            Cache cache,
            RuntimeException exception
    ) {
        log.warn(
                "Cache {} failed for cache={}; continuing without cache",
                operation,
                cache.getName(),
                exception
        );
    }
}