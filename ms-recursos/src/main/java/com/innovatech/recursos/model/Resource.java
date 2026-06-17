package com.innovatech.recursos.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad Recurso — representa un profesional disponible en Innovatech.
 * Patrón Repository: accedida exclusivamente a través de IResourceRepository.
 *
 * @author Benjamin Valdes, Ignacio Munoz
 */
@Entity
@Table(name = "resources",
       uniqueConstraints = @UniqueConstraint(columnNames = "email"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Email
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ResourceRole role = ResourceRole.DEVELOPER;

    @Column(nullable = false)
    @Builder.Default
    private boolean available = true;

    private String skills;  // CSV: "Java,Spring,React"

    @Column(name = "assigned_project_id")
    private Long assignedProjectId;

    @Column(name = "assigned_project_name")
    private String assignedProjectName;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum ResourceRole {
        DEVELOPER, DESIGNER, QA, DEVOPS, MANAGER, CONSULTANT
    }
}