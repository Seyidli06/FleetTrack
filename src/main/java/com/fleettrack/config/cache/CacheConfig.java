package com.fleettrack.config.cache;

import com.fleettrack.common.cache.CacheNames;
import com.fleettrack.driver.dto.DriverResponse;
import com.fleettrack.location.dto.VehicleLocationResponse;
import com.fleettrack.vehicle.dto.VehicleResponse;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@EnableCaching
public class CacheConfig
        implements CachingConfigurer {

    private static final Duration VEHICLE_CACHE_TTL =
            Duration.ofMinutes(10);

    private static final Duration DRIVER_CACHE_TTL =
            Duration.ofMinutes(10);

    private static final Duration LATEST_LOCATION_CACHE_TTL =
            Duration.ofSeconds(30);

    @Override
    public CacheErrorHandler errorHandler() {
        return new FailOpenCacheErrorHandler();
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer
    redisCacheManagerBuilderCustomizer(
            ObjectMapper objectMapper
    ) {
        return builder -> builder

                .transactionAware()

                .withCacheConfiguration(
                        CacheNames.VEHICLE_BY_ID,
                        createConfiguration(
                                objectMapper,
                                VehicleResponse.class,
                                VEHICLE_CACHE_TTL
                        )
                )

                .withCacheConfiguration(
                        CacheNames.DRIVER_BY_ID,
                        createConfiguration(
                                objectMapper,
                                DriverResponse.class,
                                DRIVER_CACHE_TTL
                        )
                )

                .withCacheConfiguration(
                        CacheNames.LATEST_VEHICLE_LOCATION,
                        createConfiguration(
                                objectMapper,
                                VehicleLocationResponse.class,
                                LATEST_LOCATION_CACHE_TTL
                        )
                );
    }

    private <T> RedisCacheConfiguration createConfiguration(
            ObjectMapper objectMapper,
            Class<T> type,
            Duration ttl
    ) {
        JacksonJsonRedisSerializer<T> serializer =
                new JacksonJsonRedisSerializer<>(
                        objectMapper,
                        type
                );

        return RedisCacheConfiguration
                .defaultCacheConfig()

                .entryTtl(
                        ttl
                )

                .disableCachingNullValues()

                .serializeValuesWith(
                        RedisSerializationContext
                                .SerializationPair
                                .fromSerializer(
                                        serializer
                                )
                );
    }
}