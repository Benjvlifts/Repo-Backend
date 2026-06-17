package com.innovatech.proyectos.dto;

import com.innovatech.proyectos.model.Project;
import com.innovatech.proyectos.model.ProjectNote;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTOs del microservicio ms-proyectos.
 * Incluye DTOs para notas de avance (sistema similar a PR reviews de GitHub).
 */
public class ProjectDtos {

    // ── Request: Creación de proyecto ────────────────────────────────────────
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateProjectRequest {
        @NotBlank(message = "El nombre del proyecto es obligatorio")
        private String name;

        private String description;

        @NotNull(message = "El tipo de proyecto es obligatorio")
        private Project.ProjectType type;

        private Project.ProjectStatus status;

        private Long managerId;
        private LocalDate startDate;
        private LocalDate endDate;

        // SOFTWARE
        private String techStack;
        private String repositoryUrl;

        // CONSULTING
        private String clientName;
        private Integer slaDays;

        // INFRASTRUCTURE
        private String cloudProvider;
        private Double budgetUsd;
    }

    // ── Request: Actualización de estado ─────────────────────────────────────
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateStatusRequest {
        @NotNull(message = "El nuevo estado es obligatorio")
        private Project.ProjectStatus status;
    }

    // ── Request: Edición de proyecto (Admin) ─────────────────────────────────
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateProjectRequest {
        private String name;
        private String description;
        private Project.ProjectStatus status;
    }

    // ── Request: Asignar empleado a proyecto (Admin + Manager) ───────────────
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignEmployeeRequest {
        @NotNull(message = "El ID del empleado es obligatorio")
        private Long employeeId;

        @NotBlank(message = "El nombre del empleado es obligatorio")
        private String employeeName;
    }

    // ── Request: Agregar nota de avance (Employee) ───────────────────────────
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateNoteRequest {
        @NotBlank(message = "El contenido de la nota es obligatorio")
        private String content;
    }

    // ── Request: Revisar nota (Admin + Manager) ───────────────────────────────
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewNoteRequest {
        @NotNull(message = "El estado de revisión es obligatorio")
        private ProjectNote.NoteStatus status; // APPROVED o REJECTED

        private String reviewComment;
    }

    // ── Response: Proyecto ────────────────────────────────────────────────────
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectResponse {
        private Long id;
        private String name;
        private String description;
        private String type;
        private String status;
        private Long managerId;
        private Long assignedUserId;
        private String assignedUserName;
        private LocalDate startDate;
        private LocalDate endDate;
        private String techStack;
        private String repositoryUrl;
        private String clientName;
        private Integer slaDays;
        private String cloudProvider;
        private Double budgetUsd;
        private LocalDateTime createdAt;
    }

    // ── Response: Nota de avance ──────────────────────────────────────────────
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NoteResponse {
        private Long id;
        private Long projectId;
        private Long authorId;
        private String authorName;
        private String content;
        private String status;
        private Long reviewerId;
        private String reviewerName;
        private String reviewComment;
        private LocalDateTime createdAt;
        private LocalDateTime reviewedAt;
    }

    // ── Response: Resumen ────────────────────────────────────────────────────
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectSummary {
        private Long id;
        private String name;
        private String type;
        private String status;
        private Long managerId;
        private Long assignedUserId;
        private String assignedUserName;
    }
}