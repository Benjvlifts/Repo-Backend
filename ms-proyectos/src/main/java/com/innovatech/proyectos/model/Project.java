package com.innovatech.proyectos.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidad Proyecto - representa un proyecto gestionado en la plataforma Innovatech.
 * Soporta los tipos: SOFTWARE, CONSULTING, INFRASTRUCTURE (Factory Method Pattern).
 * Ahora incluye asignación de un empleado real (userId del ms-auth).
 *
 * @author Benjamin Valdes, Ignacio Munoz
 */
@Entity
@Table(name = "projects")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private ProjectType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ProjectStatus status = ProjectStatus.PLANNING;

    @Column(name = "manager_id")
    private Long managerId;

    // Empleado asignado al proyecto (viene del ms-auth)
    @Column(name = "assigned_user_id")
    private Long assignedUserId;

    @Column(name = "assigned_user_name")
    private String assignedUserName;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    // Atributos específicos de proyectos SOFTWARE
    @Column(name = "tech_stack")
    private String techStack;

    @Column(name = "repository_url")
    private String repositoryUrl;

    // Atributos específicos de proyectos CONSULTING
    @Column(name = "client_name")
    private String clientName;

    @Column(name = "sla_days")
    private Integer slaDays;

    // Atributos específicos de proyectos INFRASTRUCTURE
    @Column(name = "cloud_provider")
    private String cloudProvider;

    @Column(name = "budget_usd")
    private Double budgetUsd;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum ProjectType {
        SOFTWARE, CONSULTING, INFRASTRUCTURE
    }

    public enum ProjectStatus {
        PLANNING, IN_PROGRESS, ON_HOLD, COMPLETED, CANCELLED
    }
}