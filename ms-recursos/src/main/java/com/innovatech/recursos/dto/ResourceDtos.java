package com.innovatech.recursos.dto;

import com.innovatech.recursos.model.Resource;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

public class ResourceDtos {

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CreateResourceRequest {
        @NotBlank(message = "El nombre es obligatorio")       private String name;
        @Email @NotBlank(message = "El email es obligatorio") private String email;
        @NotBlank(message = "El departamento es obligatorio") private String department;
        private Resource.ResourceRole role;
        private String skills;
        /** NUEVO: ID del usuario en ms-auth. Necesario para la correlación Kafka automática. */
        private Long userId;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AssignToProjectRequest {
        @NotNull(message = "El ID del proyecto es obligatorio")
        private Long projectId;

        @NotBlank(message = "El nombre del proyecto es obligatorio")
        private String projectName;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ResourceResponse {
        private Long   id;
        private String name;
        private String email;
        private String department;
        private String role;
        private boolean available;
        private String skills;
        private Long   assignedProjectId;
        private String assignedProjectName;
        /** NUEVO: expuesto para que el frontend pueda vincular con el perfil del usuario. */
        private Long   userId;
        private LocalDateTime createdAt;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class UpdateAvailabilityRequest {
        private boolean available;
    }
}