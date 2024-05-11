package com.example.automatedgradingsystembackend.controller;

import com.example.automatedgradingsystembackend.security.JwtService;
import com.example.automatedgradingsystembackend.service.GradeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/grade")
public class GradeController {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private GradeService gradeService;
    private static final Logger logger = LoggerFactory.getLogger(GradeController.class);

    @PostMapping("/upload")
    public ResponseEntity<Void> uploadMultipleFiles(@RequestParam("files[]") MultipartFile[] files) {
        try {
            gradeService.uploadMultipleFiles(files);
        } catch (Exception e) {
            logger.error("An error occurred: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        return ResponseEntity.ok().build();
    }
}
