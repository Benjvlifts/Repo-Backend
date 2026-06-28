package com.innovatech.proyectos.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * FIX Bug 1: filtro que rechaza peticiones sin JWT válido (firma verificada).
 * Reemplaza el .permitAll() ciego de SecurityConfig.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtExtractor jwtExtractor;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getServletPath();
        if (isPublic(path)) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (!jwtExtractor.isValidToken(authHeader)) {
            log.warn("[JwtAuthFilter] Rechazado — token ausente o inválido. Path={}", path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                "{\"error\":\"Unauthorized\",\"message\":\"Token JWT inválido o ausente\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isPublic(String path) {
        return path.endsWith("/health")
            || path.startsWith("/swagger-ui")
            || path.startsWith("/v3/api-docs");
    }
}