package com.example.automatedgradingsystembackend.redis;


import com.example.automatedgradingsystembackend.repository.ProjectInfoRepository;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Component
public class RedisKeyExpirationListener extends KeyExpirationEventMessageListener {

    public RedisKeyExpirationListener(RedisMessageListenerContainer listenerContainer, RedisTemplate<String, ProjectConfigForRedis> redisTemplate) {
        super(listenerContainer);
        this.redisTemplate = redisTemplate;
    }


    @Autowired
    ProjectInfoRepository projectInfoRepository;

    @Value("${redis.timeout}")
    private long timeout;

    private final RedisTemplate<String, ProjectConfigForRedis> redisTemplate;

    private static final Logger logger = LoggerFactory.getLogger(RedisKeyExpirationListener.class);

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();
        if (!expiredKey.startsWith("Time")) {
            return;
        }
        String projectId = expiredKey.substring(4);
        try {
            redisTemplate.opsForValue().set(expiredKey, ProjectConfigForRedis.builder().build(), timeout, TimeUnit.SECONDS);
            String projectConfig = redisTemplate.opsForValue().get(projectId).getProjectConfig();
            String projectPath = projectInfoRepository.findByProjectId(Long.valueOf(projectId)).getPath();
            File file = new File(projectPath);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileUtils.writeStringToFile(file, projectConfig, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Redis operation failed", e);
        }
    }
}