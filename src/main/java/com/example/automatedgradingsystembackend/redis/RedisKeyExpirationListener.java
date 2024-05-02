package com.example.automatedgradingsystembackend.redis;


import com.example.automatedgradingsystembackend.service.ProduceServiceImpl;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.TimeoutUtils;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Component
public class RedisKeyExpirationListener extends KeyExpirationEventMessageListener {

    public RedisKeyExpirationListener(RedisMessageListenerContainer listenerContainer) {
        super(listenerContainer);
    }

    @Value("${projects.path}")
    String projectsPath;

    @Value("${redis.timeout}")
    private long timeout;


    @Autowired
    private RedisTemplate<String, ProjectConfigForRedis> redisTemplate;

    private static final Logger logger = LoggerFactory.getLogger(RedisKeyExpirationListener.class);

    @Override
    public void onMessage(Message message, byte[] pattern) {
        logger.debug("key expired");
        //持久化
        String expiredKey = message.toString();
        if (!expiredKey.startsWith("Time")) {
            return;
        }
        String projectId = expiredKey.substring(4);
        try {
            redisTemplate.opsForValue().set(expiredKey, ProjectConfigForRedis.builder().build(), timeout, TimeUnit.SECONDS);
            String projectConfig = redisTemplate.opsForValue().get(projectId).getProjectConfig();
            String projectPath = projectsPath.concat(projectId).concat(".json");
            File file = new File(projectPath);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileUtils.writeStringToFile(file, projectConfig, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error(String.valueOf(redisTemplate == null));
            logger.error(e.getMessage());
        }
    }
}