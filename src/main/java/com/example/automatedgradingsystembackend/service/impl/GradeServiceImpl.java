package com.example.automatedgradingsystembackend.service.impl;

import com.example.automatedgradingsystembackend.domain.*;
import com.example.automatedgradingsystembackend.dto.response.GetBatchGradeInfoResponseDTO;
import com.example.automatedgradingsystembackend.redis.ProjectConfigForRedis;
import com.example.automatedgradingsystembackend.repository.OriginalImagesInfoRepository;
import com.example.automatedgradingsystembackend.repository.ProjectInfoRepository;
import com.example.automatedgradingsystembackend.repository.UploadBatchRepository;
import com.example.automatedgradingsystembackend.repository.UserRepository;
import com.example.automatedgradingsystembackend.service.GradeService;
import com.example.automatedgradingsystembackend.vo.GradeOverviewVo;
import com.example.automatedgradingsystembackend.vo.StudentGradeInfoVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Service
public class GradeServiceImpl implements GradeService {

    @Autowired
    UploadBatchRepository uploadBatchRepository;
    @Autowired
    OriginalImagesInfoRepository originalImagesInfoRepository;
    @Autowired
    ProjectInfoRepository projectInfoRepository;
    @Autowired
    UserRepository userRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedisTemplate<String, ProjectConfigForRedis> redisTemplate;

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


    @Override
    public void uploadMultipleFiles(MultipartFile[] files, String username) {
        Set<OriginalImagesInfo> originalImagesInfos = HashSet.newHashSet(files.length);
        for (MultipartFile _ : files) {
            originalImagesInfos.add(new OriginalImagesInfo());
        }
        UserInfo userInfo = userRepository.findByUsername(username);
        UploadBatchInfo uploadBatchInfo = UploadBatchInfo.builder()
                .timestamp(LocalDateTime.now())
                .originalImagesInfos(originalImagesInfos)
                .state("正在批改")
                .userInfo(userInfo)
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

    @Override
    public Set<GradeOverviewVo> gradeOverview(String username) {

        UserInfo userInfo = userRepository.findByUsername(username);
        List<UploadBatchInfo> uploadBatchInfos = uploadBatchRepository.findByUserInfo(userInfo);
        Set<GradeOverviewVo> gradeOverviewVos = HashSet.newHashSet(uploadBatchInfos.size());
        uploadBatchInfos.forEach(uploadBatchInfo -> {
            GradeOverviewVo gradeOverviewVo = GradeOverviewVo.builder()
                    .batchNumber(uploadBatchInfo.getBatchNumber())
                    .timestamp(uploadBatchInfo.getTimestamp())
                    .numOfUploadImages(uploadBatchInfo.getNumOfTotal())
                    .numOfSucceedProcessImages(uploadBatchInfo.getNumOfSuccess())
                    .state(uploadBatchInfo.getState())
                    .title(getProjectTitle(uploadBatchInfo.getProjectInfo()))
                    .build();
            gradeOverviewVos.add(gradeOverviewVo);
        });
        return gradeOverviewVos;
    }

    @Override
    public GetBatchGradeInfoResponseDTO getBatchGradeInfo(long batchNumber) {
        List<StudentGradeInfoVO> studentGradeInfoVOS = new ArrayList<>();

        UploadBatchInfo uploadBatchInfo = uploadBatchRepository.findByBatchNumber(batchNumber);

        Map<String, List<OriginalImagesInfo>> originalImagesInfosMapByStudentId =
                uploadBatchInfo.getOriginalImagesInfos().stream().collect(Collectors.groupingBy(OriginalImagesInfo::getStudentId));

        originalImagesInfosMapByStudentId.forEach((studentId, originalImagesInfos) -> {

            StudentGradeInfoVO studentGradeInfoVO = StudentGradeInfoVO.builder()
                    .studentId(studentId)
                    .studentName(originalImagesInfos.getFirst().getStudentName())
                    .build();
            studentGradeInfoVOS.add(studentGradeInfoVO);

            Map<Integer, BigDecimal> scores = originalImagesInfos.stream()
                    .flatMap(originalImagesInfo -> originalImagesInfo.getProcessedImagesInfos().stream())
                    .collect(Collectors.toMap(
                            ProcessedImagesInfo::getAnswerNumber,
                            ProcessedImagesInfo::getScore
                    ));
            BigDecimal scoreAddUp = scores.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            studentGradeInfoVO.setScores(scores);
            studentGradeInfoVO.setScoreAddUp(scoreAddUp);
        });
        Map<StudentGradeInfoVO, Set<Integer>> studentGradeInfoVOAnswerNumberSetMap =
                studentGradeInfoVOS.stream().collect(Collectors.toMap(
                        studentGradeInfoVO -> studentGradeInfoVO,
                        studentGradeInfoVO -> studentGradeInfoVO.getScores().keySet()
                ));
        int maxQuestionNumber = getMaxQuestionNumber(uploadBatchInfo.getProjectInfo());
        studentGradeInfoVOAnswerNumberSetMap.forEach((studentGradeInfoVO, set) -> {
            studentGradeInfoVO.setIfComplete("完整");
            for (int i = 1; i < maxQuestionNumber; i++) {
                if (!set.contains(i)) {
                    studentGradeInfoVO.setIfComplete("缺项");
                }
            }
        });

        GetBatchGradeInfoResponseDTO getBatchGradeInfoResponseDTO = GetBatchGradeInfoResponseDTO
                .builder()
                .studentGradeInfoVOs(studentGradeInfoVOS)
                .maxAnswerNumber(maxQuestionNumber)
                .build();
        return getBatchGradeInfoResponseDTO;
    }

    private int getMaxQuestionNumber(ProjectInfo projectInfo) {
        String projectConfig = null;
        String projectPath = projectInfo.getPath();
        ProjectConfigForRedis projectConfigForRedis = redisTemplate.opsForValue()
                .get(String.valueOf(projectInfo.getProjectId()));
        if (projectConfigForRedis != null) {
            projectConfig = projectConfigForRedis.getProjectConfig();
        } else {
            try {
                projectConfig = FileUtils.readFileToString(new File(projectPath), StandardCharsets.UTF_8);
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = null;
        try {
            rootNode = objectMapper.readTree(projectConfig);
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage());
        }
        JsonNode answerAreas = rootNode.get("answerAreas");
        JsonNode answers = answerAreas.get(answerAreas.size() - 1).get("answers");
        int questionNumber = answers.get(answers.size() - 1).get("questionNumber").asInt();
        return questionNumber;
    }

    private String getProjectTitle(ProjectInfo projectInfo) {
        String projectConfig = null;
        String projectPath = projectInfo.getPath();
        ProjectConfigForRedis projectConfigForRedis = redisTemplate.opsForValue()
                .get(String.valueOf(projectInfo.getProjectId()));
        if (projectConfigForRedis != null) {
            projectConfig = projectConfigForRedis.getProjectConfig();
        } else {
            try {
                projectConfig = FileUtils.readFileToString(new File(projectPath), StandardCharsets.UTF_8);
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = null;
        try {
            rootNode = objectMapper.readTree(projectConfig);
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage());
        }
        String title = rootNode.get("title").asText();
        return title;
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
        AtomicInteger numOfSuccess = new AtomicInteger();

        originalImagesInfos.forEach(originalImagesInfo -> {
            try {
                long indexOfSheets = parsingQRCode(originalImagesInfo, uploadBatchProjectInfo);
                loadStudentInfoInOriginalImages(originalImagesInfo);
                cuttingImages(originalImagesInfo, uploadBatchProjectInfo, indexOfSheets, uploadBatchInfo);
                scoreImages(originalImagesInfo);
                originalImagesInfo.setSuccessfulProcess(true);
                numOfSuccess.getAndIncrement();
            } catch (HandleImagesException exception) {
                originalImagesInfo.setSuccessfulProcess(false);
                originalImagesInfo.setFailedReason(exception.getMessage());
                logger.error(exception.getMessage());
            }
            originalImagesInfoRepository.save(originalImagesInfo);
        });
        int numOfTotal = originalImagesInfos.size();
        if (numOfSuccess.get() != numOfTotal) {
            uploadBatchInfo.setState("部分答题卡处理失败");
        } else {
            uploadBatchInfo.setState("批改完成");
        }
        uploadBatchInfo.setNumOfSuccess(numOfSuccess.get());
        uploadBatchInfo.setNumOfTotal(numOfTotal);
        uploadBatchRepository.save(uploadBatchInfo);
    }

    private void scoreImages(OriginalImagesInfo originalImagesInfo) {
        Set<ProcessedImagesInfo> processedImagesInfos = originalImagesInfo.getProcessedImagesInfos();
        //todo
        processedImagesInfos.forEach(processedImagesInfo -> {
            processedImagesInfo.setScore(new BigDecimal(10));
        });
    }

    private void cuttingImages(OriginalImagesInfo originalImagesInfo, ProjectInfo projectInfo, long indexOfSheets, UploadBatchInfo uploadBatchInfo) throws HandleImagesException {
        String projectConfigPath = projectInfo.getPath();

        String imagePath = originalImagesInfo.getPath();
        Path path = Paths.get(processedImages, STR."\{uploadBatchInfo.getBatchNumber()}", STR."\{originalImagesInfo.getOriginalImagesInfoId()}");

        ProcessBuilder processBuilder =
                new ProcessBuilder(pythonInterpreterPath, cuttingImagesPath, projectConfigPath, String.valueOf(indexOfSheets)
                        , imagePath, path.toString());
        //processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                String line = reader.readLine();

                List<Integer> answerNumbers = new ArrayList<>();
                Pattern pattern = Pattern.compile("\\d+");
                Matcher matcher = pattern.matcher(line);
                while (matcher.find()) {
                    answerNumbers.add(Integer.parseInt(matcher.group()));
                }
                Set<ProcessedImagesInfo> processedImagesInfos = HashSet.newHashSet(answerNumbers.size());
                answerNumbers.forEach(number -> {
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
