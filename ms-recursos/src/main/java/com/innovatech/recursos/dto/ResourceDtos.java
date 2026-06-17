package com.innovatech.recursos.dto;

import com.innovatech.recursos.model.Resource;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

public class ResourceDtos {

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CreateResourceRequest {
        @NotBlank(message = "El nombre es obligatorio")
        private String name;

        @Email(message = "Formato de email inválido")
        @NotBlank(message = "El email es obligatorio")
        private String email;

        @NotBlank(message = "El departamento es obligatorio")
        private String department;

        private Resource.ResourceRole role;
        private String skills;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ResourceResponse {
        private Long id;
        private String name;
        private String email;
        private String department;
        private String role;
        private boolean available;
        private String skills;
        private Long assignedProjectId;
        private String assignedProjectName;
        private LocalDateTime createdAt;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class UpdateAvailabilityRequest {
        private boolean available;
    }
}