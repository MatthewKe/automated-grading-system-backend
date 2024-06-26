package com.example.automatedgradingsystembackend.dto.request;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class CommitProjectRequestDTO {
    private long projectId;
    private String projectConfig;
    private long timestamp;
}
