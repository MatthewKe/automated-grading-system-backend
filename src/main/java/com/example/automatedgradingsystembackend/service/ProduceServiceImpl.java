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
import java.util.ArrayList;
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
            String projectPath = projectsPath.concat(String.valueOf(projectInfo.getId())).concat(".json");
            String projectContext = null;
            try {
                logger.info("projectPath:" + projectPath);
                projectContext = FileUtils.readFileToString(new File(projectPath), StandardCharsets.UTF_8);
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
            projectConfigs.put(projectInfo.getId(), projectContext);
        });

        return projectConfigs;
    }

    @Override
    public long createProject(String username) {
        long id = -1;
        UserInfo userInfo = userRepository.findByUsername(username);
        ProjectInfo projectInfo = ProjectInfo.builder()
                .user(userInfo)
                .build();
        projectRepository.save(projectInfo);
        return id;
    }
}
