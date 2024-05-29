package com.example.automatedgradingsystembackend.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public interface ProduceService {
    public Map<Long, String> getProjectConfigsByUsername(String username);

    public long createProject(String username, long timestamp);

    public void commitProject(String username, String projectConfig, long projectId, long timestamp) throws IOException;

    public String getProjectConfig(long projectId);

    public boolean userHasProjectAuthority(String username, long projectId);

    public InputStream download(String jwt, long projectId);

}
