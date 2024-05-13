package com.example.automatedgradingsystembackend.service;

import com.example.automatedgradingsystembackend.domain.UploadBatchInfo;
import com.example.automatedgradingsystembackend.dto.response.GetBatchGradeInfoResponseDTO;
import com.example.automatedgradingsystembackend.vo.GradeOverviewVo;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface GradeService {
    public void uploadMultipleFiles(MultipartFile[] files, String username);


    Set<GradeOverviewVo> gradeOverview(String username);

    GetBatchGradeInfoResponseDTO getBatchGradeInfo(long batchNumber);
}
