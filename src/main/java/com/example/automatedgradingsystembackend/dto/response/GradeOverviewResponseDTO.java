package com.example.automatedgradingsystembackend.dto.response;


import com.example.automatedgradingsystembackend.vo.GradeOverviewVo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GradeOverviewResponseDTO {

    private Set<GradeOverviewVo> gradeOverviewVos;
}


