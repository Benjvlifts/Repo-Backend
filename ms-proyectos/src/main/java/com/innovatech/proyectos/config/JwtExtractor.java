package com.innovatech.proyectos.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * FIX Bug 1: reemplaza decodificación manual de Base64 (sin verificación de firma)
 * por verificación HMAC-SHA usando la clave compartida con ms-auth.
 * Un JWT forjado con rol=ADMIN ahora es rechazado antes de llegar al controlador.
 */
@Slf4j
@Component
public class JwtExtractor {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    // ── Verificación de firma ─────────────────────────────────────────────────

    public boolean isValidToken(String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) return false;
            Jwts.parser().verifyWith(getSigningKey()).build()
                    .parseSignedClaims(authHeader.substring(7));
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("[JwtExtractor] Token inválido: {}", e.getMessage());
            return false;
        }
    }

    // ── Extracción de claims (solo si firma es válida) ────────────────────────

    public Map<String, Object> extractClaims(String authHeader) {
        if (!isValidToken(authHeader)) return Collections.emptyMap();
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey()).build()
                    .parseSignedClaims(authHeader.substring(7))
                    .getPayload();
            return new HashMap<>(claims);
        } catch (Exception e) {
            log.warn("[JwtExtractor] Error extrayendo claims: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    public String extractRole(String authHeader) {
        Object role = extractClaims(authHeader).get("role");
        return role != null ? role.toString() : "";
    }

    public Long extractUserId(String authHeader) {
        Object userId = extractClaims(authHeader).get("userId");
        if (userId instanceof Integer i) return i.longValue();
        if (userId instanceof Long l)    return l;
        if (userId instanceof Number n)  return n.longValue();
        return null;
    }

    public String extractSubject(String authHeader) {
        Object sub = extractClaims(authHeader).get("sub");
        return sub != null ? sub.toString() : "";
    }

    // ── Helpers de rol ────────────────────────────────────────────────────────

    public boolean isAdmin(String authHeader) {
        return "ADMIN".equalsIgnoreCase(extractRole(authHeader));
    }

    public boolean isManager(String authHeader) {
        return "MANAGER".equalsIgnoreCase(extractRole(authHeader));
    }

    public boolean isEmployee(String authHeader) {
        return "EMPLOYEE".equalsIgnoreCase(extractRole(authHeader));
    }

    public boolean isAdminOrManager(String authHeader) {
        String role = extractRole(authHeader);
        return "ADMIN".equalsIgnoreCase(role) || "MANAGER".equalsIgnoreCase(role);
    }
}