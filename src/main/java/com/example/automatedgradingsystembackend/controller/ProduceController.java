package com.example.automatedgradingsystembackend.controller;

import com.example.automatedgradingsystembackend.model.UserInfo;
import com.example.automatedgradingsystembackend.model.request.LoginRequestDTO;
import com.example.automatedgradingsystembackend.model.request.RegisterRequestDTO;
import com.example.automatedgradingsystembackend.model.response.*;
import com.example.automatedgradingsystembackend.security.JwtService;
import com.example.automatedgradingsystembackend.service.ProduceService;
import com.example.automatedgradingsystembackend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

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

    @GetMapping("/overview")
    public ResponseEntity<ProduceOverviewResponseDTO> overview(HttpServletRequest request) {
        String username = jwtService.extractUsernameFromHttpServletRequest(request);
        if (username != null) {
            Map<Long, String> projectConfigs = produceService.getProjectConfigsByUsername(username);
            ProduceOverviewResponseDTO produceOverviewResponseDTO = ProduceOverviewResponseDTO.builder().projectConfigs(projectConfigs).build();
            return ResponseEntity.ok(produceOverviewResponseDTO);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/createProject")
    public ResponseEntity<CreateProjectResponseDTO> createProject(HttpServletRequest request) {
        String username = jwtService.extractUsernameFromHttpServletRequest(request);
        if (username != null) {
            long id = produceService.createProject(username);
            CreateProjectResponseDTO createProjectResponseDTO = CreateProjectResponseDTO.builder()
                    .id(id)
                    .build();
            return ResponseEntity.ok(createProjectResponseDTO);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }


}
