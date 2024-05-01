package com.example.automatedgradingsystembackend.service;

import com.example.automatedgradingsystembackend.model.ProjectInfo;
import com.example.automatedgradingsystembackend.model.UserInfo;
import com.example.automatedgradingsystembackend.repository.ProjectRepository;
import com.example.automatedgradingsystembackend.repository.UserRepository;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProduceServiceImpl implements ProduceService {


    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    UserRepository userRepository;

    @Value("${projects.path}")
    String projectsPath;
    private static final Logger logger = LoggerFactory.getLogger(ProduceServiceImpl.class);

    @Override
    public Map<Long, String> getProjectConfigsByUsername(String username) {
        Map<Long, String> projectConfigs = new HashMap<>();
        List<ProjectInfo> projectInfos = projectRepository.findByUserUsername(username);

        projectInfos.forEach(projectInfo -> {
            String projectPath = projectsPath.concat(String.valueOf(projectInfo.getProjectId())).concat(".json");
            String projectContext = null;
            try {
                logger.info("projectPath:" + projectPath);
                projectContext = FileUtils.readFileToString(new File(projectPath), StandardCharsets.UTF_8);
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
            projectConfigs.put(projectInfo.getProjectId(), projectContext);
        });

        return projectConfigs;
    }

    @Override
    public long createProject(String username) {

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
        
        commitProject(username, projectConfig, projectId);
        return projectId;
    }

    @Override
    public boolean commitProject(String username, String projectConfig, long projectId) {
        try {
            String projectPath = projectsPath.concat(String.valueOf(projectId)).concat(".json");
            File file = new File(projectPath);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileUtils.writeStringToFile(file, projectConfig, StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            logger.error(e.getMessage());
            return false;
        }
    }

    @Override
    public String getProjectConfig(long projectId) {
        String projectPath = projectsPath.concat(String.valueOf(projectId)).concat(".json");
        String projectContext = null;
        try {
            logger.info("projectPath:" + projectPath);
            projectContext = FileUtils.readFileToString(new File(projectPath), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        return projectContext;
    }

    @Override
    public boolean testProjectIdMatchesUser(String username, long projectId) {
        ProjectInfo projectInfo = projectRepository.findByProjectIdAndUserUsername(projectId, username);
        if (projectInfo == null) {
            return false;
        }
        return true;
    }


}
