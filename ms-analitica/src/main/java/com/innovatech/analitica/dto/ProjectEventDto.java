package com.innovatech.analitica.dto;

import lombok.Data;

@Data
public class ProjectEventDto {
    private Long projectId;
    private String eventType; // ej. "PROJECT_CREATED", "STATUS_UPDATED", "TASK_COMPLETED"
    private String status;
    private Double completionPercentage;
    private Integer activeTasks;
}