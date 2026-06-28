package com.innovatech.analitica.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// DTO FALTANTE — su ausencia impedía la compilación de ms-analitica
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KpiSummaryResponse {
    private int totalProjects;
    private double averageCompletion;
    private long activeProjects;
    private long completedProjects;
    private List<KpiResponse> metrics;
}