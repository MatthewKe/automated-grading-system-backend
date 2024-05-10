package com.example.automatedgradingsystembackend.controller;

import com.example.automatedgradingsystembackend.dto.request.CommitProjectRequestDTO;
import com.example.automatedgradingsystembackend.dto.response.CreateProjectResponseDTO;
import com.example.automatedgradingsystembackend.dto.response.GetProjectConfigResponseDTO;
import com.example.automatedgradingsystembackend.dto.response.ProduceOverviewResponseDTO;
import com.example.automatedgradingsystembackend.security.JwtService;
import com.example.automatedgradingsystembackend.service.ProduceService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Controller
@RequestMapping("/produce")
public class ProduceController {
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ProduceService produceService;

    private static final Logger logger = LoggerFactory.getLogger(ProduceController.class);

    @GetMapping("/overview")
    public ResponseEntity<ProduceOverviewResponseDTO> overview(HttpServletRequest request) {
        String username = jwtService.extractUsernameFromHttpServletRequest(request);
        Map<Long, String> projectConfigs = produceService.getProjectConfigsByUsername(username);
        for (Map.Entry<Long, String> entry : projectConfigs.entrySet()) {
            String projectConfig = entry.getValue();
            if (!isValidJson(projectConfig)) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }
        ProduceOverviewResponseDTO produceOverviewResponseDTO = ProduceOverviewResponseDTO.builder().projectConfigs(projectConfigs).build();
        return ResponseEntity.ok(produceOverviewResponseDTO);
    }

    @GetMapping("/createProject")
    public ResponseEntity<CreateProjectResponseDTO> createProject(HttpServletRequest request, @RequestParam long timestamp) {
        String username = jwtService.extractUsernameFromHttpServletRequest(request);
        long id = produceService.createProject(username, timestamp);
        CreateProjectResponseDTO createProjectResponseDTO = CreateProjectResponseDTO.builder()
                .id(id)
                .build();
        return ResponseEntity.ok(createProjectResponseDTO);
    }


    private static boolean isValidJson(String json) {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
        try {
            engine.eval("JSON.parse(" + "JSON.stringify(" + json + "));");
            return true;
        } catch (ScriptException e) {
            logger.error("not Valid Json");
            logger.error(json);
            logger.error(e.toString());
            return false;
        }

    }

    @PostMapping("/commitProject")
    public ResponseEntity<Void> commitProject(HttpServletRequest request, @RequestBody CommitProjectRequestDTO commitProjectRequestDTO) throws IOException {
        logger.debug("commitProject begins");
        if (!isValidJson(commitProjectRequestDTO.getProjectConfig())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        String username = jwtService.extractUsernameFromHttpServletRequest(request);
        if (!produceService.testProjectIdMatchesUser(username, commitProjectRequestDTO.getProjectId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        produceService.commitProject(username, commitProjectRequestDTO.getProjectConfig(), commitProjectRequestDTO.getProjectId(), commitProjectRequestDTO.getTimestamp());
        logger.debug("commitProject ends");
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/getProjectConfig")
    public ResponseEntity<GetProjectConfigResponseDTO> getProjectConfig(HttpServletRequest request, @RequestParam long projectId) {
        String username = jwtService.extractUsernameFromHttpServletRequest(request);
        if (!produceService.testProjectIdMatchesUser(username, projectId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String projectConfig = produceService.getProjectConfig(projectId);
        if (!isValidJson(projectConfig)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        GetProjectConfigResponseDTO getProjectConfigResponseDTO = GetProjectConfigResponseDTO.builder()
                .projectConfig(projectConfig)
                .build();
        return ResponseEntity.ok(getProjectConfigResponseDTO);
    }

    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> download(HttpServletRequest request, @RequestParam long projectId) {
        String username = jwtService.extractUsernameFromHttpServletRequest(request);
        if (!produceService.testProjectIdMatchesUser(username, projectId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        InputStream inputStream = produceService.download(jwtService.extractTokenFromHttpServletRequest(request), projectId);
        InputStreamResource inputStreamResource = new InputStreamResource(inputStream);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=\"download.pdf\"")
                .body(inputStreamResource);
    }

}
