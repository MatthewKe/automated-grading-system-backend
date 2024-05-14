package com.example.automatedgradingsystembackend.controller;

import com.example.automatedgradingsystembackend.domain.ProcessedImageInfo;
import com.example.automatedgradingsystembackend.dto.response.GetBatchGradeInfoResponseDTO;
import com.example.automatedgradingsystembackend.dto.response.GetStudentGradeInfoResponseDTO;
import com.example.automatedgradingsystembackend.dto.response.GradeOverviewResponseDTO;
import com.example.automatedgradingsystembackend.security.JwtService;
import com.example.automatedgradingsystembackend.service.GradeService;
import com.example.automatedgradingsystembackend.vo.GradeOverviewVo;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
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

    @PostMapping("/updateScore")
    public ResponseEntity<Void> updateScore(HttpServletRequest request, @RequestBody ProcessedImageInfo processedImageInfo) {
        try {
            String username = jwtService.extractUsernameFromHttpServletRequest(request);
            gradeService.updateScore(processedImageInfo, username);
        } catch (Exception e) {
            logger.error("An error occurred: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        return ResponseEntity.ok().build();
    }


    @GetMapping("/overview")
    public ResponseEntity<GradeOverviewResponseDTO> overview(HttpServletRequest request) {
        try {
            String username = jwtService.extractUsernameFromHttpServletRequest(request);
            Set<GradeOverviewVo> gradeOverviewVoSet = gradeService.gradeOverview(username);
            GradeOverviewResponseDTO gradeOverviewResponseDTO = GradeOverviewResponseDTO.builder()
                    .gradeOverviewVos(gradeOverviewVoSet)
                    .build();
            return ResponseEntity.ok(gradeOverviewResponseDTO);
        } catch (Exception e) {
            logger.error("An error occurred: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }

    @GetMapping("/getBatchGradeInfo")
    public ResponseEntity<GetBatchGradeInfoResponseDTO> getBatchGradeInfo(HttpServletRequest request, @RequestParam long batchNumber) {
        try {
            GetBatchGradeInfoResponseDTO getBatchGradeInfoResponseDTO = gradeService.getBatchGradeInfo(batchNumber);
            return ResponseEntity.ok(getBatchGradeInfoResponseDTO);
        } catch (Exception e) {
            logger.error("An error occurred: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/getStudentGradeInfo")
    public ResponseEntity<GetStudentGradeInfoResponseDTO> getStudentGradeInfo(HttpServletRequest request, @RequestParam long batchNumber, @RequestParam String studentId) {
        try {
            GetStudentGradeInfoResponseDTO getStudentGradeInfoResponseDTO = gradeService.getStudentGradeInfo(batchNumber, studentId);
            return ResponseEntity.ok(getStudentGradeInfoResponseDTO);
        } catch (Exception e) {
            logger.error("An error occurred: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/getStudentOriginalImageIds")
    public ResponseEntity<List<Long>> getStudentOriginalImageIds(HttpServletRequest request, @RequestParam long batchNumber, @RequestParam String studentId) {

        try {
            List<Long> list = gradeService.getStudentOriginalImageIds(batchNumber, studentId);
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            logger.error("An error occurred: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/getOriginalImage")
    public ResponseEntity<Resource> getOriginalImage(HttpServletRequest request, @RequestParam Long originalImageId) {
        try {
            Resource resource = gradeService.getOriginalImageResource(originalImageId);
            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (Exception e) {
            logger.error("An error occurred: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/getStudentProcessedImageIds")
    public ResponseEntity<Set<ProcessedImageInfo>> getStudentProcessedImageIds(HttpServletRequest request, @RequestParam long batchNumber, @RequestParam String studentId) {

        try {
            Set<ProcessedImageInfo> processedImageInfos = gradeService.getStudentProcessedImageIds(batchNumber, studentId);
            return ResponseEntity.ok(processedImageInfos);
        } catch (Exception e) {
            logger.error("An error occurred: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/getProcessedImage")
    public ResponseEntity<Resource> getProcessedImage(HttpServletRequest request, @RequestParam Long processedImageId) {
        try {
            Resource resource = gradeService.getProceesedImageResource(processedImageId);
            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (Exception e) {
            logger.error("An error occurred: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
