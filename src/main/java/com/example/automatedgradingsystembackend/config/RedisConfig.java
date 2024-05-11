package com.example.automatedgradingsystembackend.config;

import com.example.automatedgradingsystembackend.redis.ProjectConfigForRedis;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, ProjectConfigForRedis> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, ProjectConfigForRedis> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(ProjectConfigForRedis.class));
        return template;
    }

    @Bean
    public RedisMessageListenerContainer keyExpirationListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer listenerContainer = new RedisMessageListenerContainer();
        listenerContainer.setConnectionFactory(connectionFactory);
        //listenerContainer.addMessageListener(new RedisKeyExpirationListener(listenerContainer, redisTemplate(connectionFactory)), new PatternTopic("__keyevent@*:expired"));
        return listenerContainer;
    }

}
