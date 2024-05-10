package com.example.automatedgradingsystembackend.controller;

import com.example.automatedgradingsystembackend.model.request.CommitProjectRequestDTO;
import com.example.automatedgradingsystembackend.model.response.CreateProjectResponseDTO;
import com.example.automatedgradingsystembackend.model.response.GetProjectConfigResponseDTO;
import com.example.automatedgradingsystembackend.model.response.ProduceOverviewResponseDTO;
import com.example.automatedgradingsystembackend.security.JwtService;
import com.example.automatedgradingsystembackend.service.GradeService;
import com.example.automatedgradingsystembackend.service.ProduceService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

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
