package com.example.automatedgradingsystembackend.service.impl;

import com.example.automatedgradingsystembackend.domain.ProjectInfo;
import com.example.automatedgradingsystembackend.repository.OriginalImagesInfoRepository;
import com.example.automatedgradingsystembackend.repository.ProjectInfoRepository;
import com.example.automatedgradingsystembackend.repository.UploadBatchRepository;
import com.example.automatedgradingsystembackend.domain.OriginalImagesInfo;
import com.example.automatedgradingsystembackend.domain.UploadBatchInfo;
import com.example.automatedgradingsystembackend.service.GradeService;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class GradeServiceImpl implements GradeService {

    @Autowired
    UploadBatchRepository uploadBatchRepository;
    @Autowired
    OriginalImagesInfoRepository originalImagesInfoRepository;
    @Autowired
    ProjectInfoRepository projectInfoRepository;

    private static final Logger logger = LoggerFactory.getLogger(GradeServiceImpl.class);

    @Value("${originalImages.path}")
    private String originalImagesPath;

    @Value("${python.scanQrCode.path}")
    private String scanQrCodePythonPath;

    @Value("${python.interpreter.path}")
    private String pythonInterpreterPath;

    @Override
    public void uploadMultipleFiles(MultipartFile[] files) {
        Set<OriginalImagesInfo> originalImagesInfos = HashSet.newHashSet(files.length);
        for (MultipartFile _ : files) {
            originalImagesInfos.add(new OriginalImagesInfo());
        }
        UploadBatchInfo uploadBatchInfo = UploadBatchInfo.builder()
                .timestamp(LocalDateTime.now())
                .originalImagesInfos(originalImagesInfos)
                .build();
        uploadBatchInfo = uploadBatchRepository.save(uploadBatchInfo);
        List<OriginalImagesInfo> originalImagesInfoArr = uploadBatchInfo.getOriginalImagesInfos().stream().toList();
        ProjectInfo projectInfo = null;
        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            OriginalImagesInfo originalImagesInfo = originalImagesInfoArr.get(i);
            Path path = Paths.get(originalImagesPath, STR."\{uploadBatchInfo.getBatchNumber()}", STR."\{originalImagesInfo.getOriginalImagesInfoId()}.png");
            originalImagesInfo.setPath(path.toString());
            originalImagesInfoRepository.save(originalImagesInfo);
            saveOriginalImages(file, path);
            if (projectInfo == null) {
                projectInfo = getProjectInfo(path);
                if (projectInfo != null) {
                    uploadBatchInfo.setProjectInfo(projectInfo);
                    uploadBatchRepository.save(uploadBatchInfo);
                }
            }
        }
        
    }

    private ProjectInfo getProjectInfo(Path path) {
        ProcessBuilder processBuilder =
                new ProcessBuilder(pythonInterpreterPath, scanQrCodePythonPath, path.toString());
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                long projectId = Long.parseLong(line.split("-")[0]);
                return projectInfoRepository.findByProjectId(projectId);
            } else {
                logger.error("scanQrCodePython failed, exitCode is {}", exitCode);
                return null;
            }
        } catch (IOException | InterruptedException e) {
            logger.error(e.getLocalizedMessage());
        }

        return null;
    }


    private void saveOriginalImages(MultipartFile file, Path path) {
        try {
            Files.createDirectories(path.getParent());
            File fileToBeWritten = new File(path.toString());
            if (!fileToBeWritten.exists()) {
                fileToBeWritten.createNewFile();
            }
            byte[] bytes = file.getBytes();
            FileUtils.writeByteArrayToFile(fileToBeWritten, bytes);
        } catch (IOException e) {
            logger.error("An error occurred: {}", e.getMessage(), e);
        }
    }
}
