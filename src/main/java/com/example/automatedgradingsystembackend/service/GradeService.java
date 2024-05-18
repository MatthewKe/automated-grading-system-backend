package com.example.automatedgradingsystembackend.service;

import com.example.automatedgradingsystembackend.domain.ProcessedImageInfo;
import com.example.automatedgradingsystembackend.dto.response.GetBatchGradeInfoResponseDTO;
import com.example.automatedgradingsystembackend.dto.response.GetStudentGradeInfoResponseDTO;
import com.example.automatedgradingsystembackend.vo.GradeOverviewVo;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Set;

public interface GradeService {
    public void uploadMultipleFiles(MultipartFile[] files, String username);


    Set<GradeOverviewVo> gradeOverview(String username);

    GetBatchGradeInfoResponseDTO getBatchGradeInfo(long batchNumber);

    GetStudentGradeInfoResponseDTO getStudentGradeInfo(long batchNumber, String studentId);

    List<Long> getStudentOriginalImageIds(long batchNumber, String studentId);

    Resource getOriginalImageResource(Long originalImageId) throws MalformedURLException;

    Set<ProcessedImageInfo> getStudentProcessedImageIds(long batchNumber, String studentId);

    Resource getProceesedImageResource(Long processedImageId) throws MalformedURLException;

    void updateScore(ProcessedImageInfo processedImagesInfo, String username);
}
