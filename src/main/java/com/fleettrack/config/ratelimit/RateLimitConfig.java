package com.fleettrack.config.ratelimit;

import com.fleettrack.ratelimit.config.RateLimitProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(
        RateLimitProperties.class
)
public class RateLimitConfig {

    @Bean
    public RedisScript<String> rateLimitScript() {

        String script = """
                local current = redis.call('INCR', KEYS[1])

                if current == 1 then
                    redis.call('EXPIRE', KEYS[1], ARGV[1])
                end

                local ttl = redis.call('TTL', KEYS[1])

                return tostring(current) .. ':' .. tostring(ttl)
                """;

        DefaultRedisScript<String> redisScript =
                new DefaultRedisScript<>();

        redisScript.setScriptText(
                script
        );

        redisScript.setResultType(
                String.class
        );

        return redisScript;
    }
}