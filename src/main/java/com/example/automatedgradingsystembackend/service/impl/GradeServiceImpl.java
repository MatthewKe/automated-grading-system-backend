package com.example.automatedgradingsystembackend.service.impl;

import com.example.automatedgradingsystembackend.domain.ProcessedImagesInfo;
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
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
public class GradeServiceImpl implements GradeService {

    @Autowired
    UploadBatchRepository uploadBatchRepository;
    @Autowired
    OriginalImagesInfoRepository originalImagesInfoRepository;
    @Autowired
    ProjectInfoRepository projectInfoRepository;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final Logger logger = LoggerFactory.getLogger(GradeServiceImpl.class);

    @Value("${originalImages.path}")
    private String originalImagesPath;

    @Value("${processedImages.path}")
    private String processedImages;

    @Value("${python.scanQrCode.path}")
    private String scanQrCodePythonPath;

    @Value("${python.interpreter.path}")
    private String pythonInterpreterPath;

    @Value("${python.cuttingImages.path}")
    private String cuttingImagesPath;

    @Value("${projects.path}")
    String projectsPath;

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

        boolean loadProjectInfoUnUploadBatchSuccess = false;
        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            OriginalImagesInfo originalImagesInfo = originalImagesInfoArr.get(i);
            Path path = Paths.get(originalImagesPath, STR."\{uploadBatchInfo.getBatchNumber()}", STR."\{originalImagesInfo.getOriginalImagesInfoId()}.png");
            originalImagesInfo.setPath(path.toString());
            originalImagesInfoRepository.save(originalImagesInfo);
            saveOriginalImages(file, path);
            if (!loadProjectInfoUnUploadBatchSuccess) {
                loadProjectInfoUnUploadBatchSuccess = loadProjectInfoInUploadBatch(path, uploadBatchInfo);
            }
        }
        rabbitTemplate.convertAndSend("uploadBatchInfo", uploadBatchInfo.getBatchNumber());
    }

    private boolean loadProjectInfoInUploadBatch(Path path, UploadBatchInfo uploadBatchInfo) {
        ProjectInfo projectInfo = null;
        String QrInfo = scanQrCode(path.toString());
        if (QrInfo == null) {
            return false;
        }
        long projectId = 0;
        try {
            projectId = Long.parseLong(QrInfo.split("-")[0]);
        } catch (Exception e) {
            return false;
        }
        projectInfo = projectInfoRepository.findByProjectId(projectId);
        if (projectInfo == null) {
            return false;
        }
        uploadBatchInfo.setProjectInfo(projectInfo);
        uploadBatchRepository.save(uploadBatchInfo);
        return true;
    }

    @RabbitListener(queues = "uploadBatchInfo")
    private void handleImages(long batchNumber) {
        logger.info("handleImages start");
        UploadBatchInfo uploadBatchInfo = uploadBatchRepository.findUploadBatchInfoByBatchNumber(batchNumber);
        Set<OriginalImagesInfo> originalImagesInfos = uploadBatchInfo.getOriginalImagesInfos();
        ProjectInfo uploadBatchProjectInfo = uploadBatchInfo.getProjectInfo();
        originalImagesInfos.forEach(originalImagesInfo -> {
            try {
                long indexOfSheets = parsingQRCode(originalImagesInfo, uploadBatchProjectInfo);
                loadStudentInfoInOriginalImages(originalImagesInfo);
                cuttingImages(originalImagesInfo, uploadBatchProjectInfo, indexOfSheets, uploadBatchInfo);
                scoreImages(originalImagesInfo);
                originalImagesInfo.setSuccessfulProcess(true);
            } catch (HandleImagesException exception) {
                originalImagesInfo.setSuccessfulProcess(false);
                originalImagesInfo.setFailedReason(exception.getMessage());
                logger.error(exception.getMessage());
            }
            originalImagesInfoRepository.save(originalImagesInfo);
        });
    }

    private void scoreImages(OriginalImagesInfo originalImagesInfo) {
        Set<ProcessedImagesInfo> processedImagesInfos = originalImagesInfo.getProcessedImagesInfos();
        //todo
        processedImagesInfos.forEach(processedImagesInfo -> {
            processedImagesInfo.setScore(new BigDecimal(10));
        });
    }

    private void cuttingImages(OriginalImagesInfo originalImagesInfo, ProjectInfo projectInfo, long indexOfSheets, UploadBatchInfo uploadBatchInfo) throws HandleImagesException {
        String projectConfigPath = projectsPath.concat(String.valueOf(projectInfo.getProjectId())).concat(".json");
        String imagePath = originalImagesInfo.getPath();
        Path path = Paths.get(processedImages, STR."\{uploadBatchInfo.getBatchNumber()}", STR."\{originalImagesInfo.getOriginalImagesInfoId()}");

        ProcessBuilder processBuilder =
                new ProcessBuilder(pythonInterpreterPath, cuttingImagesPath, projectConfigPath, String.valueOf(indexOfSheets)
                        , imagePath, path.toString());
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                String line = reader.readLine();
                List<Integer> numbers = new ArrayList<>();
                Pattern pattern = Pattern.compile("\\d+");
                Matcher matcher = pattern.matcher(line);
                while (matcher.find()) {
                    numbers.add(Integer.parseInt(matcher.group()));
                }
                Set<ProcessedImagesInfo> processedImagesInfos = HashSet.newHashSet(numbers.size());
                numbers.forEach(number -> {
                    ProcessedImagesInfo processedImagesInfo = ProcessedImagesInfo.builder()
                            .answerNumber(number)
                            .path(path + "\\" + number + ".png")
                            .build();
                    processedImagesInfos.add(processedImagesInfo);
                });
                originalImagesInfo.setProcessedImagesInfos(processedImagesInfos);
            } else {
                logPythonScriptError("cuttingImages", reader, exitCode);
                throw new HandleImagesException("切割答题卡失败");
            }
        } catch (IOException | InterruptedException e) {
            logger.error(e.getLocalizedMessage());
            throw new HandleImagesException("切割答题卡失败");
        }
    }

    private void logPythonScriptError(String functionName, BufferedReader reader, int exitCode) throws IOException {
        String line = null;
        StringBuilder stringBuilder = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
            stringBuilder.append("/n");
        }
        logger.error(stringBuilder.toString());
        logger.error("{} failed, exitCode is {}", functionName, exitCode);
    }


    private void loadStudentInfoInOriginalImages(OriginalImagesInfo originalImagesInfo) {
        //todo
        originalImagesInfo.setStudentId("201180082");
        originalImagesInfo.setStudentName("柯科");
        originalImagesInfoRepository.save(originalImagesInfo);
    }

    private long parsingQRCode(OriginalImagesInfo originalImagesInfo, ProjectInfo uploadBatchProjectInfo) throws HandleImagesException {
        String QrCodeInfo = scanQrCode(originalImagesInfo.getPath());
        if (QrCodeInfo == null) {
            throw new HandleImagesException("无法正确解析二维码获取答题卡项目信息");
        }
        long projectId = 0;
        try {
            projectId = Long.parseLong(QrCodeInfo.split("-")[0]);
        } catch (Exception e) {
            throw new HandleImagesException("无法正确解析二维码获取答题卡项目信息");
        }
        ProjectInfo projectInfo = projectInfoRepository.findByProjectId(projectId);
        if (!projectInfo.equals(uploadBatchProjectInfo)) {
            throw new HandleImagesException("该图片的答题卡项目信息与该批次的答题卡项目信息不符");
        }
        long indexOfSheets = 0;
        try {
            indexOfSheets = Long.parseLong(QrCodeInfo.split("-")[1]);
        } catch (Exception e) {
            throw new HandleImagesException("无法正确解析二维码获取答题卡项目信息");
        }
        return indexOfSheets;
    }

    private String scanQrCode(String path) {
        ProcessBuilder processBuilder =
                new ProcessBuilder(pythonInterpreterPath, scanQrCodePythonPath, path);
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                String line = reader.readLine();
                return line;
            } else {
                logPythonScriptError("scanQrCode", reader, exitCode);
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
