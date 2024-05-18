package com.example.automatedgradingsystembackend.service.impl;

import com.example.automatedgradingsystembackend.domain.ProjectInfo;
import com.example.automatedgradingsystembackend.redis.ProjectConfigForRedis;
import com.example.automatedgradingsystembackend.service.ProjectService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class ProjectServiceImpl implements ProjectService {
    @Autowired
    private RedisTemplate<String, ProjectConfigForRedis> redisTemplate;

    private static final Logger logger = LoggerFactory.getLogger(ProjectServiceImpl.class);

    @Override
    public String getProjectTitle(ProjectInfo projectInfo) {
        if (projectInfo == null) {
            return null;
        }
        String projectConfig = null;
        String projectPath = projectInfo.getPath();
        ProjectConfigForRedis projectConfigForRedis = redisTemplate.opsForValue()
                .get(String.valueOf(projectInfo.getProjectId()));
        if (projectConfigForRedis != null) {
            projectConfig = projectConfigForRedis.getProjectConfig();
        } else {
            try {
                projectConfig = FileUtils.readFileToString(new File(projectPath), StandardCharsets.UTF_8);
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = null;
        try {
            rootNode = objectMapper.readTree(projectConfig);
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage());
        }
        String title = rootNode.get("title").asText();
        return title;
    }
}
