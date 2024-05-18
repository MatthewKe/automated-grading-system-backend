package com.example.automatedgradingsystembackend.dto.response;


import com.example.automatedgradingsystembackend.vo.StudentGradeInfoVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GetBatchGradeInfoResponseDTO {
    private List<StudentGradeInfoVO> studentGradeInfoVOs = new ArrayList<>();
    private int maxAnswerNumber;
    private Set<FailedOriginalImageInfo> failedOriginalImageInfos = new HashSet<>();

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class FailedOriginalImageInfo {
        long failedOriginalImageId;
        String failedReason;
    }
}





