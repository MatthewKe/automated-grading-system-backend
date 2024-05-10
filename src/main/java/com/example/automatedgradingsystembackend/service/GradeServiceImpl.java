package com.example.automatedgradingsystembackend.service;

import com.example.automatedgradingsystembackend.repository.OriginalImagesInfoRepository;
import com.example.automatedgradingsystembackend.repository.UploadBatchRepository;
import com.example.automatedgradingsystembackend.repository.OriginalImagesInfo;
import com.example.automatedgradingsystembackend.repository.UploadBatchInfo;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
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

    private static final Logger logger = LoggerFactory.getLogger(GradeServiceImpl.class);

    @Value("${originalImages.path}")
    private String originalImagesPath;

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
        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            OriginalImagesInfo originalImagesInfo = originalImagesInfoArr.get(i);
            Path path = Paths.get(originalImagesPath, STR."\{uploadBatchInfo.getBatchNumber()}", STR."\{originalImagesInfo.getOriginalImagesInfoId()}.png");
            originalImagesInfo.setPath(path.toString());
            originalImagesInfoRepository.save(originalImagesInfo);
            saveOriginalImages(file, path);
        }
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
