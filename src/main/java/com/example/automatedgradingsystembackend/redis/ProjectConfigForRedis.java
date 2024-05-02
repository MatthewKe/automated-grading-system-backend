package com.example.automatedgradingsystembackend.redis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectConfigForRedis {
    private String projectConfig;
    private long timestamp;
}
