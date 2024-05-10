package com.example.automatedgradingsystembackend.security;

import com.example.automatedgradingsystembackend.service.impl.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JwtAuthFilter extends OncePerRequestFilter {

    private JwtService jwtService;

    UserDetailsServiceImpl userDetailsServiceImpl;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = jwtService.extractTokenFromHttpServletRequest(request);
        if (token == null) {
            logger.error("No JWT token found in request headers");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("No JWT token found in request headers");
            return;
        }
        String username = jwtService.extractUsernameFromHttpServletRequest(request);
        if (username == null) {
            logger.error("No username found in request headers");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("No username found in request headers");
            return;
        }
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsServiceImpl.loadUserByUsername(username);
            if (!jwtService.validateToken(token, userDetails)) {
                logger.error("Token and userDetails no match in request headers");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Token and userDetails no match in request headers");
                return;
            }
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }
        filterChain.doFilter(request, response);
    }
}