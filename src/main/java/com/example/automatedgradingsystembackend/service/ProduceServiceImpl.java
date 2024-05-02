package com.example.automatedgradingsystembackend.service;

import com.example.automatedgradingsystembackend.model.ProjectInfo;
import com.example.automatedgradingsystembackend.model.UserInfo;
import com.example.automatedgradingsystembackend.redis.ProjectConfigForRedis;
import com.example.automatedgradingsystembackend.repository.ProjectRepository;
import com.example.automatedgradingsystembackend.repository.UserRepository;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class ProduceServiceImpl implements ProduceService {


    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    UserRepository userRepository;


    @Value("${projects.path}")
    String projectsPath;

    @Autowired
    private RedisTemplate<String, ProjectConfigForRedis> redisTemplate;

    @Value("${redis.timeout}")
    private long timeout;

    private static final Logger logger = LoggerFactory.getLogger(ProduceServiceImpl.class);

    @Override
    public Map<Long, String> getProjectConfigsByUsername(String username) {
        Map<Long, String> projectConfigs = new HashMap<>();
        List<ProjectInfo> projectInfos = projectRepository.findByUserUsername(username);

        projectInfos.forEach(projectInfo -> {
            String projectId = String.valueOf(projectInfo.getProjectId());
            String projectContext = getProjectConfig(projectId);
            projectConfigs.put(projectInfo.getProjectId(), projectContext);
        });

        return projectConfigs;
    }

    @Override
    public long createProject(String username, long timestamp) {
        UserInfo userInfo = userRepository.findByUsername(username);
        ProjectInfo projectInfo = ProjectInfo.builder()
                .user(userInfo)
                .build();
        projectRepository.save(projectInfo);
        long projectId = projectInfo.getProjectId();

        String projectConfig = String.format("{\n" +
                "  \"projectId\": %d,\n" +
                "  \"title\": \"答题卡（点击我修改）\",\n" +
                "  \"sizeOfSheet\": \"A3\",\n" +
                "  \"sheetsPadding\": 14,\n" +
                "  \"sheets\": [\n" +
                "    {\n" +
                "      \"numOfAnswerAreaContainers\": 3\n" +
                "    },\n" +
                "    {\n" +
                "      \"numOfAnswerAreaContainers\": 2\n" +
                "    }\n" +
                "  ],\n" +
                "  \"gapBetweenAnswerAreaContainer\": 3,\n" +
                "  \"answerAreaContainerPadding\": 3,\n" +
                "  \"gapBetweenAnswerArea\": 3,\n" +
                "  \"nextId\": 0,\n" +
                "  \"defaultFontWidth\": 6.5,\n" +
                "  \"answerAreas\": []\n" +
                "}", projectId);
        String projectPath = projectsPath.concat(String.valueOf(projectId)).concat(".json");
        File file = new File(projectPath);
        try {
            file.createNewFile();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        commitProject(username, projectConfig, projectId, timestamp);
        return projectId;
    }

    @Override
    public void commitProject(String username, String projectConfig, long projectId, long timestamp) {
        ProjectConfigForRedis projectConfigForRedis = ProjectConfigForRedis.builder()
                .projectConfig(projectConfig)
                .timestamp(timestamp)
                .build();
        setProjectConfig(String.valueOf(projectId), projectConfigForRedis);
    }

    @Override
    public String getProjectConfig(long projectId) {
        return getProjectConfig(String.valueOf(projectId));
    }

    @Override
    public boolean testProjectIdMatchesUser(String username, long projectId) {
        ProjectInfo projectInfo = projectRepository.findByProjectIdAndUserUsername(projectId, username);
        if (projectInfo == null) {
            return false;
        }
        return true;
    }


    private void setProjectConfig(String key, ProjectConfigForRedis value) {
        redisTemplate.execute(new SessionCallback<Object>() {
            @Override
            public <K, V> Object execute(RedisOperations<K, V> operations) throws DataAccessException {
                operations.multi();
                RedisOperations<String, ProjectConfigForRedis> ops = (RedisOperations<String, ProjectConfigForRedis>) operations;
                ProjectConfigForRedis oldValue = ops.opsForValue().get(key);
                if (oldValue == null || oldValue.getTimestamp() < value.getTimestamp()) {
                    ops.opsForValue().set(key, value);
                    ops.opsForValue().set(STR."Time\{key}", ProjectConfigForRedis.builder().build(), timeout, TimeUnit.SECONDS);
                }
                return ops.exec();
            }
        });
    }

    private String getProjectConfig(String projectConfigId) {
        ProjectConfigForRedis projectConfigForRedis = redisTemplate.opsForValue().get(projectConfigId);
        if (projectConfigForRedis != null) {
            return projectConfigForRedis.getProjectConfig();
        }
        String projectPath = projectsPath.concat(projectConfigId).concat(".json");
        String projectConfig = null;
        try {
            projectConfig = FileUtils.readFileToString(new File(projectPath), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        redisTemplate.opsForValue().set(projectConfigId, ProjectConfigForRedis.builder()
                .timestamp(0)
                .projectConfig(projectConfig)
                .build());
        return projectConfig;
    }
}
