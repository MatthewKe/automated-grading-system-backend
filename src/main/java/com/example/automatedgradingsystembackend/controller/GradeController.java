package com.example.automatedgradingsystembackend.controller;

import com.example.automatedgradingsystembackend.domain.UploadBatchInfo;
import com.example.automatedgradingsystembackend.dto.response.GetBatchGradeInfoResponseDTO;
import com.example.automatedgradingsystembackend.dto.response.GradeOverviewResponseDTO;

import com.example.automatedgradingsystembackend.security.JwtService;
import com.example.automatedgradingsystembackend.service.GradeService;
import com.example.automatedgradingsystembackend.vo.GradeOverviewVo;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/grade")
public class GradeController {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private GradeService gradeService;
    private static final Logger logger = LoggerFactory.getLogger(GradeController.class);

    @PostMapping("/upload")
    public ResponseEntity<Void> uploadMultipleFiles(HttpServletRequest request, @RequestParam("files[]") MultipartFile[] files) {
        try {
            String username = jwtService.extractUsernameFromHttpServletRequest(request);
            gradeService.uploadMultipleFiles(files, username);
        } catch (Exception e) {
            logger.error("An error occurred: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/overview")
    public ResponseEntity<GradeOverviewResponseDTO> overview(HttpServletRequest request) {
        Set<GradeOverviewVo> gradeOverviewVoSet;
        try {
            String username = jwtService.extractUsernameFromHttpServletRequest(request);
            gradeOverviewVoSet = gradeService.gradeOverview(username);
        } catch (Exception e) {
            logger.error("An error occurred: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        GradeOverviewResponseDTO gradeOverviewResponseDTO = GradeOverviewResponseDTO.builder()
                .gradeOverviewVos(gradeOverviewVoSet)
                .build();
        return ResponseEntity.ok(gradeOverviewResponseDTO);
    }

    @GetMapping("/getBatchGradeInfo")
    public ResponseEntity<GetBatchGradeInfoResponseDTO> getBatchGradeInfo(HttpServletRequest request, @RequestParam long batchNumber) {
        GetBatchGradeInfoResponseDTO getBatchGradeInfoResponseDTO;
        try {
            getBatchGradeInfoResponseDTO = gradeService.getBatchGradeInfo(batchNumber);
        } catch (Exception e) {
            logger.error("An error occurred: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        return ResponseEntity.ok(getBatchGradeInfoResponseDTO);
    }
}
