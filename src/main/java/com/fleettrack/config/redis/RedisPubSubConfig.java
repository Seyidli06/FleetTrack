package com.fleettrack.config.redis;

import com.fleettrack.notification.config.NotificationProperties;
import com.fleettrack.notification.subscriber.RedisNotificationSubscriber;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(
        NotificationProperties.class
)
public class RedisPubSubConfig {

    @Bean
    public ChannelTopic notificationTopic(
            NotificationProperties properties
    ) {
        return new ChannelTopic(
                properties.channel()
        );
    }

    @Bean
    public RedisMessageListenerContainer
    redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisNotificationSubscriber subscriber,
            ChannelTopic notificationTopic
    ) {
        RedisMessageListenerContainer container =
                new RedisMessageListenerContainer();

        container.setConnectionFactory(
                connectionFactory
        );

        container.addMessageListener(
                subscriber,
                notificationTopic
        );

        return container;
    }
}