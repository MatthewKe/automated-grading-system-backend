package com.example.automatedgradingsystembackend.service.impl;

import com.example.automatedgradingsystembackend.domain.*;
import com.example.automatedgradingsystembackend.dto.response.GetBatchGradeInfoResponseDTO;
import com.example.automatedgradingsystembackend.dto.response.GetStudentGradeInfoResponseDTO;
import com.example.automatedgradingsystembackend.redis.ProjectConfigForRedis;
import com.example.automatedgradingsystembackend.repository.*;
import com.example.automatedgradingsystembackend.service.GradeService;
import com.example.automatedgradingsystembackend.service.ProjectService;
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
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Service
public class GradeServiceImpl implements GradeService {

    @Autowired
    UploadBatchRepository uploadBatchRepository;
    @Autowired
    OriginalImageInfoRepository originalImageInfoRepository;
    @Autowired
    ProjectInfoRepository projectInfoRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    ProcessedImageInfoRepository processedImageInfoRepository;


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

    @Autowired
    ProjectService projectService;

    @Autowired
    private RestTemplate restTemplate;


    @Override
    public void uploadMultipleFiles(MultipartFile[] files, String username) {
        Set<OriginalImageInfo> originalImageInfos = HashSet.newHashSet(files.length);
        for (MultipartFile _ : files) {
            originalImageInfos.add(new OriginalImageInfo());
        }
        UserInfo userInfo = userRepository.findByUsername(username);
        UploadBatchInfo uploadBatchInfo = UploadBatchInfo.builder()
                .timestamp(ZonedDateTime.now().toLocalDateTime())
                .originalImageInfos(originalImageInfos)
                .state("正在批改")
                .userInfo(userInfo)
                .build();
        uploadBatchInfo = uploadBatchRepository.save(uploadBatchInfo);
        List<OriginalImageInfo> originalImageInfoArr = uploadBatchInfo.getOriginalImageInfos().stream().toList();

        boolean loadProjectInfoUnUploadBatchSuccess = false;
        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            OriginalImageInfo originalImageInfo = originalImageInfoArr.get(i);
            Path path = Paths.get(originalImagesPath, STR."\{uploadBatchInfo.getBatchNumber()}", STR."\{originalImageInfo.getOriginalImagesInfoId()}.png");
            originalImageInfo.setPath(path.toString());
            originalImageInfoRepository.save(originalImageInfo);
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
                    .title(projectService.getProjectTitle(uploadBatchInfo.getProjectInfo()))
                    .build();
            gradeOverviewVos.add(gradeOverviewVo);
        });
        return gradeOverviewVos;
    }

    @Override
    public GetBatchGradeInfoResponseDTO getBatchGradeInfo(long batchNumber) {
        UploadBatchInfo uploadBatchInfo = uploadBatchRepository.findByBatchNumber(batchNumber);
        Set<OriginalImageInfo> originalImageInfos = uploadBatchInfo.getOriginalImageInfos();
        Set<GetBatchGradeInfoResponseDTO.FailedOriginalImageInfo> failedOriginalImageInfos = originalImageInfos.stream()
                .filter(Predicate.not(OriginalImageInfo::isSuccessfulProcess))
                .map(originalImageInfo -> GetBatchGradeInfoResponseDTO.FailedOriginalImageInfo.builder()
                        .failedOriginalImageId(originalImageInfo.getOriginalImagesInfoId())
                        .failedReason(originalImageInfo.getFailedReason())
                        .build())
                .collect(Collectors.toSet());
        if (uploadBatchInfo.getProjectInfo() == null) {
            return GetBatchGradeInfoResponseDTO.builder()
                    .failedOriginalImageInfos(failedOriginalImageInfos)
                    .build();
        }
        List<StudentGradeInfoVO> studentGradeInfoVOS = new ArrayList<>();
        Map<String, List<OriginalImageInfo>> originalImagesInfosMapByStudentId =
                uploadBatchInfo.getOriginalImageInfos().stream().filter(info -> info.getStudentId() != null).collect(Collectors.groupingBy(OriginalImageInfo::getStudentId));
        originalImagesInfosMapByStudentId.forEach((studentId, originalImagesInfos) -> {
            StudentGradeInfoVO studentGradeInfoVO = StudentGradeInfoVO.builder()
                    .studentId(studentId)
                    .studentName(originalImagesInfos.getFirst().getStudentName())
                    .build();
            studentGradeInfoVOS.add(studentGradeInfoVO);

            Map<Integer, BigDecimal> scores = originalImagesInfos.stream()
                    .flatMap(originalImageInfo -> originalImageInfo.getProcessedImageInfos().stream())
                    .collect(Collectors.toMap(
                            ProcessedImageInfo::getAnswerNumber,
                            ProcessedImageInfo::getScore
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
                    break;
                }
            }
        });

        return GetBatchGradeInfoResponseDTO
                .builder()
                .studentGradeInfoVOs(studentGradeInfoVOS)
                .failedOriginalImageInfos(failedOriginalImageInfos)
                .maxAnswerNumber(maxQuestionNumber)
                .build();
    }

    @Override
    public GetStudentGradeInfoResponseDTO getStudentGradeInfo(long batchNumber, String studentId) {
        UploadBatchInfo uploadBatchInfo = uploadBatchRepository.findByBatchNumber(batchNumber);

        List<OriginalImageInfo> originalImageInfos =
                uploadBatchInfo.getOriginalImageInfos().stream()
                        .filter(info -> info.getStudentId() != null)
                        .collect(Collectors.groupingBy(OriginalImageInfo::getStudentId))
                        .get(studentId);

        Map<Integer, BigDecimal> scores = originalImageInfos.stream()
                .flatMap(originalImageInfo -> originalImageInfo.getProcessedImageInfos().stream())
                .collect(Collectors.toMap(
                        ProcessedImageInfo::getAnswerNumber,
                        ProcessedImageInfo::getScore
                ));

        BigDecimal scoreAddUp = scores.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        StudentGradeInfoVO studentGradeInfoVO = StudentGradeInfoVO.builder()
                .studentId(originalImageInfos.getFirst().getStudentId())
                .studentName(originalImageInfos.getFirst().getStudentName())
                .scores(scores)
                .scoreAddUp(scoreAddUp)
                .build();

        int maxQuestionNumber = getMaxQuestionNumber(uploadBatchInfo.getProjectInfo());

        studentGradeInfoVO.setIfComplete("完整");
        Set<Integer> answerNumbers = scores.keySet();
        for (int i = 1; i < maxQuestionNumber; i++) {
            if (!answerNumbers.contains(i)) {
                studentGradeInfoVO.setIfComplete("缺项");
                break;
            }
        }


        return GetStudentGradeInfoResponseDTO.builder()
                .studentGradeInfoVOs(List.of(studentGradeInfoVO))
                .maxAnswerNumber(maxQuestionNumber)
                .build();
    }

    @Override
    public List<Long> getStudentOriginalImageIds(long batchNumber, String studentId) {

        UploadBatchInfo uploadBatchInfo = uploadBatchRepository.findByBatchNumber(batchNumber);
        Set<OriginalImageInfo> originalImageInfos = uploadBatchInfo.getOriginalImageInfos();
        Set<OriginalImageInfo> originalImageInfosByStudentId = originalImageInfos
                .stream()
                .filter(originalImageInfo -> originalImageInfo.getStudentId() != null)
                .filter(originalImageInfo -> originalImageInfo.getStudentId().equals(studentId))
                .collect(Collectors.toSet());
        return originalImageInfosByStudentId.stream().map(OriginalImageInfo::getOriginalImagesInfoId).collect(Collectors.toList());
    }

    @Override
    public Resource getOriginalImageResource(Long originalImageId) throws MalformedURLException {
        String path = originalImageInfoRepository.findById(originalImageId).get().getPath();
        Path file = Paths.get(path);
        Resource resource = new UrlResource(file.toUri());
        return resource;
    }

    @Override
    public Set<ProcessedImageInfo> getStudentProcessedImageIds(long batchNumber, String studentId) {
        UploadBatchInfo uploadBatchInfo = uploadBatchRepository.findByBatchNumber(batchNumber);
        Set<OriginalImageInfo> originalImageInfos = uploadBatchInfo.getOriginalImageInfos();
        Set<OriginalImageInfo> originalImageInfosByStudentId = originalImageInfos
                .stream()
                .filter(originalImageInfo -> originalImageInfo.getStudentId() != null)
                .filter(originalImageInfo -> originalImageInfo.getStudentId().equals(studentId))
                .collect(Collectors.toSet());
        Set<ProcessedImageInfo> processedImageInfos = originalImageInfosByStudentId
                .stream()
                .flatMap(originalImageInfo -> originalImageInfo.getProcessedImageInfos().stream())
                .collect(Collectors.toSet());
        return processedImageInfos;
    }

    @Override
    public Resource getProceesedImageResource(Long processedImageId) throws MalformedURLException {
        String path = processedImageInfoRepository.findById(processedImageId).get().getPath();
        Path file = Paths.get(path);
        Resource resource = new UrlResource(file.toUri());
        return resource;
    }

    @Override
    public void updateScore(ProcessedImageInfo processedImageInfo, String username) {
        processedImageInfoRepository.save(processedImageInfo);
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
        Set<OriginalImageInfo> originalImageInfos = uploadBatchInfo.getOriginalImageInfos();
        ProjectInfo uploadBatchProjectInfo = uploadBatchInfo.getProjectInfo();
        AtomicInteger numOfSuccess = new AtomicInteger();
        originalImageInfos.forEach(originalImageInfo -> {
            try {
                long indexOfSheets = parsingQRCode(originalImageInfo, uploadBatchProjectInfo);
                loadStudentInfoInOriginalImages(originalImageInfo);
                cuttingImages(originalImageInfo, uploadBatchProjectInfo, indexOfSheets, uploadBatchInfo);
                scoreImages(originalImageInfo);
                originalImageInfo.setSuccessfulProcess(true);
                numOfSuccess.getAndIncrement();
            } catch (HandleImagesException exception) {
                originalImageInfo.setSuccessfulProcess(false);
                originalImageInfo.setFailedReason(exception.getMessage());
                logger.error(exception.getMessage());
            }
        });
        int numOfTotal = originalImageInfos.size();
        if (numOfSuccess.get() != numOfTotal) {
            uploadBatchInfo.setState("部分答题卡处理失败");
        } else {
            uploadBatchInfo.setState("批改完成");
        }
        uploadBatchInfo.setNumOfSuccess(numOfSuccess.get());
        uploadBatchInfo.setNumOfTotal(numOfTotal);
        uploadBatchRepository.save(uploadBatchInfo);
    }


    private void scoreImages(OriginalImageInfo originalImageInfo) {
        Set<ProcessedImageInfo> processedImageInfos = originalImageInfo.getProcessedImageInfos();
        processedImageInfos.forEach(processedImageInfo -> processedImageInfo.setScore(BigDecimal.ZERO));
        originalImageInfo.setProcessedImageInfos(processedImageInfos);
    }

    private void cuttingImages(OriginalImageInfo originalImageInfo, ProjectInfo projectInfo, long indexOfSheets, UploadBatchInfo uploadBatchInfo) throws HandleImagesException {
        String projectConfigPath = projectInfo.getPath();

        String imagePath = originalImageInfo.getPath();
        Path path = Paths.get(processedImages, STR."\{uploadBatchInfo.getBatchNumber()}", STR."\{originalImageInfo.getOriginalImagesInfoId()}");

        ProcessBuilder processBuilder =
                new ProcessBuilder(pythonInterpreterPath, cuttingImagesPath, projectConfigPath, String.valueOf(indexOfSheets)
                        , imagePath, path.toString());
        try {
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                String line = reader.readLine();
                List<Integer> answerNumbers = new ArrayList<>();
                Pattern pattern = Pattern.compile("\\d+");
                Matcher matcher = pattern.matcher(line);
                while (matcher.find()) {
                    answerNumbers.add(Integer.parseInt(matcher.group()));
                }
                Set<ProcessedImageInfo> processedImageInfos = HashSet.newHashSet(answerNumbers.size());
                answerNumbers.forEach(number -> {
                    ProcessedImageInfo processedImageInfo = ProcessedImageInfo.builder()
                            .answerNumber(number)
                            .path(path + "\\" + number + ".png")
                            .build();
                    processedImageInfos.add(processedImageInfo);
                });
                originalImageInfo.setProcessedImageInfos(processedImageInfos);
            } else {
                logPythonScriptError("cuttingImages", errorReader, exitCode);
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


    private void loadStudentInfoInOriginalImages(OriginalImageInfo originalImageInfo) {
        //todo
        originalImageInfo.setStudentId("201180082");
        originalImageInfo.setStudentName("柯科");
        originalImageInfoRepository.save(originalImageInfo);
    }

    private long parsingQRCode(OriginalImageInfo originalImageInfo, ProjectInfo uploadBatchProjectInfo) throws HandleImagesException {
        String QrCodeInfo = scanQrCode(originalImageInfo.getPath());
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
