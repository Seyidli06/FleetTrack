package com.fleettrack.notification.subscriber;

import com.fleettrack.notification.dto.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

@Component
public class RedisNotificationSubscriber
        implements MessageListener {

    private static final Logger log =
            LoggerFactory.getLogger(
                    RedisNotificationSubscriber.class
            );

    private final ObjectMapper objectMapper;

    public RedisNotificationSubscriber(
            ObjectMapper objectMapper
    ) {
        this.objectMapper =
                objectMapper;
    }

    @Override
    public void onMessage(
            Message message,
            byte[] pattern
    ) {
        String payload =
                new String(
                        message.getBody(),
                        StandardCharsets.UTF_8
                );

        try {

            NotificationEvent event =
                    objectMapper.readValue(
                            payload,
                            NotificationEvent.class
                    );

            /*
             * Phase 14-də subscriber real notification
             * processing nöqtəsidir.
             *
             * Gələcəkdə buradan WebSocket, email və ya
             * başqa notification adapter-i çağırıla bilər.
             */
            log.info(
                    """
                    Redis notification received:
                    eventId={}
                    type={}
                    vehicleId={}
                    referenceId={}
                    title={}
                    message={}
                    occurredAt={}
                    """,
                    event.eventId(),
                    event.type(),
                    event.vehicleId(),
                    event.referenceId(),
                    event.title(),
                    event.message(),
                    event.occurredAt()
            );

        } catch (Exception exception) {

            log.error(
                    "Failed to deserialize Redis notification payload={}",
                    payload,
                    exception
            );
        }
    }
}