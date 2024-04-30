package com.example.automatedgradingsystembackend.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;


public class ExcludePathRequestMatcher implements RequestMatcher {
    private String excludePath;

    public ExcludePathRequestMatcher(String excludePath) {
        this.excludePath = excludePath;
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        return request.getServletPath().startsWith(excludePath);
    }
}
