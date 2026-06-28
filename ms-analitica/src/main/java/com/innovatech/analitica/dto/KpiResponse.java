package com.innovatech.analitica.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KpiResponse {
    private Long projectId;
    private String projectStatus;
    private Double completionPercentage;
    private Integer activeTasks;
    private LocalDateTime lastUpdated;
}