package com.example.automatedgradingsystembackend.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GradeOverviewVo {
    LocalDateTime timestamp;
    int numOfUploadImages;
    int numOfSucceedProcessImages;
    String title;
    String state;
    long batchNumber;
}