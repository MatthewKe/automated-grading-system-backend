package com.example.automatedgradingsystembackend.controller;

import com.example.automatedgradingsystembackend.model.request.CommitProjectRequestDTO;
import com.example.automatedgradingsystembackend.model.response.*;
import com.example.automatedgradingsystembackend.security.JwtService;
import com.example.automatedgradingsystembackend.service.ProduceService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
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
        ProduceOverviewResponseDTO produceOverviewResponseDTO = ProduceOverviewResponseDTO.builder().projectConfigs(projectConfigs).build();
        return ResponseEntity.ok(produceOverviewResponseDTO);
    }

    @GetMapping("/createProject")
    public ResponseEntity<CreateProjectResponseDTO> createProject(HttpServletRequest request) {
        String username = jwtService.extractUsernameFromHttpServletRequest(request);
        long id = produceService.createProject(username);
        CreateProjectResponseDTO createProjectResponseDTO = CreateProjectResponseDTO.builder()
                .id(id)
                .build();
        return ResponseEntity.ok(createProjectResponseDTO);
    }


    @PostMapping("/commitProject")
    public ResponseEntity<Void> commitProject(HttpServletRequest request, @RequestBody CommitProjectRequestDTO commitProjectRequestDTO) throws IOException {
        String username = jwtService.extractUsernameFromHttpServletRequest(request);
        logger.debug(commitProjectRequestDTO.toString());
        if (!produceService.testProjectIdMatchesUser(username, commitProjectRequestDTO.getProjectId())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        produceService.commitProject(username, commitProjectRequestDTO.getProjectConfig(), commitProjectRequestDTO.getProjectId());
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/getProjectConfig")
    public ResponseEntity<GetProjectConfigResponseDTO> getProjectConfig(HttpServletRequest request, @RequestParam long projectId) {
        String username = jwtService.extractUsernameFromHttpServletRequest(request);
        if (!produceService.testProjectIdMatchesUser(username, projectId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String projectConfig = produceService.getProjectConfig(projectId);
        GetProjectConfigResponseDTO getProjectConfigResponseDTO = GetProjectConfigResponseDTO.builder()
                .projectConfig(projectConfig)
                .build();
        return ResponseEntity.ok(getProjectConfigResponseDTO);
    }

}
