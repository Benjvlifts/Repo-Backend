package com.innovatech.analitica.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_metrics")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProjectMetric {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long projectId;

    /** NUEVO: nombre del proyecto para mostrar en el dashboard de KPIs. */
    @Column
    private String projectName;

    @Column(nullable = false)
    private String projectStatus;

    @Column(nullable = false)
    private Double completionPercentage;

    @Column(nullable = false)
    private Integer activeTasks;

    @Column(nullable = false)
    private LocalDateTime lastUpdated;
}