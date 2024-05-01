package com.example.automatedgradingsystembackend.service;

import java.io.IOException;
import java.util.Map;

public interface ProduceService {
    public Map<Long, String> getProjectConfigsByUsername(String username);

    public long createProject(String username);

    public boolean commitProject(String username, String projectConfig, long id) throws IOException;

    public String getProjectConfig(long projectId);

    public boolean testProjectIdMatchesUser(String username, long projectId);

}
