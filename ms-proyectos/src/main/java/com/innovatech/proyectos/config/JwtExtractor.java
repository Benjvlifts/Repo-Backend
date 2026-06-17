package com.innovatech.proyectos.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Collections;
import java.util.Map;

/**
 * Utilidad para extraer claims del token JWT sin depender de librerías adicionales.
 * Decodifica el payload Base64URL del JWT y parsea el JSON con Jackson (ya incluido en Spring Web).
 *
 * El JWT de ms-auth incluye: sub (email), role (ADMIN/MANAGER/EMPLOYEE), userId (Long), exp.
 */
@Component
@RequiredArgsConstructor
public class JwtExtractor {

    private final ObjectMapper objectMapper;

    /**
     * Extrae todos los claims del JWT a partir del encabezado Authorization.
     * @param authHeader valor del header "Authorization: Bearer <token>"
     * @return mapa con los claims, o mapa vacío si falla
     */
    public Map<String, Object> extractClaims(String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Collections.emptyMap();
            }
            String token = authHeader.substring(7);
            String[] parts = token.split("\\.");
            if (parts.length < 2) return Collections.emptyMap();

            // Padding para Base64URL
            String payload = parts[1];
            int pad = 4 - payload.length() % 4;
            if (pad != 4) payload = payload + "=".repeat(pad);

            byte[] decoded = Base64.getUrlDecoder().decode(payload);
            return objectMapper.readValue(decoded, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    /**
     * Extrae el rol del usuario desde el JWT.
     * @return "ADMIN", "MANAGER", "EMPLOYEE" o "" si no se puede extraer
     */
    public String extractRole(String authHeader) {
        Object role = extractClaims(authHeader).get("role");
        return role != null ? role.toString() : "";
    }

    /**
     * Extrae el ID del usuario desde el JWT.
     */
    public Long extractUserId(String authHeader) {
        Object userId = extractClaims(authHeader).get("userId");
        if (userId instanceof Integer) return ((Integer) userId).longValue();
        if (userId instanceof Long) return (Long) userId;
        if (userId instanceof Number) return ((Number) userId).longValue();
        return null;
    }

    /**
     * Extrae el nombre/email del usuario (subject del JWT).
     */
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