package com.example.automatedgradingsystembackend.service;

import com.example.automatedgradingsystembackend.model.ProjectInfo;

import java.util.List;
import java.util.Map;

public interface ProduceService {
    public Map<Long, String> getProjectConfigsByUsername(String username);

    public long createProject(String username);
}
