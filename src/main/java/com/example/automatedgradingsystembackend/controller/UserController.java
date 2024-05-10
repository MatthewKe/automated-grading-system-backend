package com.example.automatedgradingsystembackend.controller;

import com.example.automatedgradingsystembackend.domain.UserInfo;
import com.example.automatedgradingsystembackend.dto.request.LoginRequestDTO;
import com.example.automatedgradingsystembackend.dto.request.RegisterRequestDTO;
import com.example.automatedgradingsystembackend.dto.response.LoginResponseDTO;
import com.example.automatedgradingsystembackend.dto.response.RegisterResponseDTO;
import com.example.automatedgradingsystembackend.dto.response.ValidateTokenResponseDTO;
import com.example.automatedgradingsystembackend.security.JwtService;
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

@Controller
@RequestMapping("/user")
public class UserController {
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;


    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO loginRequestDTO) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequestDTO.getUsername(), loginRequestDTO.getPassword())
            );
            if (authentication.isAuthenticated()) {
                LoginResponseDTO res = LoginResponseDTO.builder()
                        .accessToken(jwtService.GenerateToken(loginRequestDTO.getUsername()))
                        .build();
                return ResponseEntity.ok(res);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> register(@RequestBody RegisterRequestDTO registerRequestDTO) {
        UserInfo userInfo = UserInfo.builder()
                .password(registerRequestDTO.getPassword())
                .username(registerRequestDTO.getUsername())
                .build();
        boolean registerSuccess = userService.registerUser(userInfo);
        if (registerSuccess) {
            RegisterResponseDTO registerResponseDTO = RegisterResponseDTO.builder()
                    .accessToken(jwtService.GenerateToken(registerRequestDTO.getUsername()))
                    .registerSuccess(true)
                    .build();
            return ResponseEntity.ok(registerResponseDTO);
        } else {
            RegisterResponseDTO registerResponseDTO = RegisterResponseDTO.builder()
                    .registerSuccess(false)
                    .build();
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @GetMapping("validate-token")
    public ResponseEntity<ValidateTokenResponseDTO> validateToken(HttpServletRequest request) {
        String username = jwtService.extractUsernameFromHttpServletRequest(request);
        ValidateTokenResponseDTO validateTokenResponseDTO = ValidateTokenResponseDTO.builder()
                .username(username)
                .build();
        return ResponseEntity.ok(validateTokenResponseDTO);

    }
}
