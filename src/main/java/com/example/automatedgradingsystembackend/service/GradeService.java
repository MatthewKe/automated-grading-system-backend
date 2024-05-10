package com.example.automatedgradingsystembackend.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public interface GradeService {
    public void uploadMultipleFiles(MultipartFile[] files);
    
}
