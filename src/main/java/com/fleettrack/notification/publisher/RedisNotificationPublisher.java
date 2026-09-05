package com.fleettrack.notification.publisher;

import com.fleettrack.notification.dto.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class RedisNotificationPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(
                    RedisNotificationPublisher.class
            );

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ChannelTopic notificationTopic;

    public RedisNotificationPublisher(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            ChannelTopic notificationTopic
    ) {
        this.redisTemplate =
                redisTemplate;

        this.objectMapper =
                objectMapper;

        this.notificationTopic =
                notificationTopic;
    }

    public void publish(
            NotificationEvent event
    ) {
        try {

            String payload =
                    objectMapper.writeValueAsString(
                            event
                    );

            Long receivers =
                    redisTemplate.convertAndSend(
                            notificationTopic.getTopic(),
                            payload
                    );

            log.info(
                    "Published Redis notification eventId={}, type={}, receivers={}",
                    event.eventId(),
                    event.type(),
                    receivers
            );

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Failed to publish Redis notification event "
                            + event.eventId(),
                    exception
            );
        }
    }
}