package com.fleettrack.notification.subscriber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
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

        handleMessage(
                payload
        );
    }

    public void handleMessage(
            String payload
    ) {
        try {
            JsonNode root =
                    objectMapper.readTree(
                            payload
                    );

            String eventId =
                    textValue(
                            root,
                            "eventId"
                    );

            String type =
                    textValue(
                            root,
                            "type"
                    );

            String vehicleId =
                    textValue(
                            root,
                            "vehicleId"
                    );

            log.info(
                    "Redis notification received, eventId={}, type={}, vehicleId={}",
                    eventId,
                    type,
                    vehicleId
            );

        } catch (
                Exception exception
        ) {
            log.error(
                    "Failed to deserialize Redis notification payload",
                    exception
            );
        }
    }

    private String textValue(
            JsonNode root,
            String fieldName
    ) {
        JsonNode value =
                root.get(
                        fieldName
                );

        if (value == null
                || value.isNull()) {

            return null;
        }

        return value.asText();
    }
}