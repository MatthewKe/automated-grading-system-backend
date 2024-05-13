package com.example.automatedgradingsystembackend.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StudentGradeInfoVO {
    private String studentName;
    private String studentId;
    private Map<Integer, BigDecimal> scores;
    private String ifComplete;
    private BigDecimal scoreAddUp;
}