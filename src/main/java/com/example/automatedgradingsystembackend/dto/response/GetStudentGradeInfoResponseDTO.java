package com.example.automatedgradingsystembackend.dto.response;


import com.example.automatedgradingsystembackend.vo.StudentGradeInfoVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GetStudentGradeInfoResponseDTO {

    private List<StudentGradeInfoVO> studentGradeInfoVOs;
    private int maxAnswerNumber;
}


