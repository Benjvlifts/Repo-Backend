package com.innovatech.auth.dto;

import com.innovatech.auth.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTOs del microservicio ms-auth.
 * Separan la capa de transporte de la capa de dominio (buena práctica DDD).
 */
public class AuthDtos {

    // ── Request: Registro de nuevo usuario ──────────────────────────────────
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterRequest {
        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 2, max = 100)
        private String name;

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "Formato de email inválido")
        private String email;

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
        private String password;

        private User.Role role;
    }

    // ── Request: Login ───────────────────────────────────────────────────────
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank(message = "El email es obligatorio")
        @Email
        private String email;

        @NotBlank(message = "La contraseña es obligatoria")
        private String password;
    }

    // ── Response: Token JWT ──────────────────────────────────────────────────
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthResponse {
        private String token;
        private String type;
        private Long userId;
        private String name;
        private String email;
        private String role;
    }

    // ── Response: Datos públicos del usuario ─────────────────────────────────
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserResponse {
        private Long id;
        private String name;
        private String email;
        private String role;
        private boolean active;
    }

    // ── Response: Mensaje genérico ───────────────────────────────────────────
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageResponse {
        private String message;
        private boolean success;
    }
}