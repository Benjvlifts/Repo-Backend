package com.innovatech.analitica.service;

import com.innovatech.analitica.dto.KpiResponse;
import com.innovatech.analitica.dto.KpiSummaryResponse;
import com.innovatech.analitica.dto.ProjectEventDto;
import com.innovatech.analitica.model.ProjectMetric;
import com.innovatech.analitica.repository.ProjectMetricRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
@Service
@Slf4j 
@RequiredArgsConstructor
public class AnaliticaService {

    private final ProjectMetricRepository metricRepository;

    @Transactional
    public void processProjectEvent(ProjectEventDto event) {
        // Ignorar eventos de recursos: no afectan métricas de proyecto
        if (event.getEventType() != null &&
            (event.getEventType().startsWith("RESOURCE_"))) {
            return;
        }

        ProjectMetric metric = metricRepository.findByProjectId(event.getProjectId())
                .orElse(ProjectMetric.builder()
                        .projectId(event.getProjectId())
                        .completionPercentage(0.0)
                        .activeTasks(0)
                        .build());

        if (event.getProjectName() != null) {
            metric.setProjectName(event.getProjectName());
        }

        if (event.getStatus() != null) {
            metric.setProjectStatus(event.getStatus());
            metric.setCompletionPercentage(computeCompletionPercentage(event.getStatus()));
            // Ajusta tareas activas según el ciclo de vida
            switch (event.getStatus().toUpperCase()) {
                case "IN_PROGRESS" -> metric.setActiveTasks(
                        Math.max(1, metric.getActiveTasks() + 1));
                case "ON_HOLD"     -> { /* mantiene activeTasks actuales */ }
                case "COMPLETED", "CANCELLED" -> metric.setActiveTasks(0);
                case "PLANNING"    -> metric.setActiveTasks(
                        metric.getActiveTasks() == 0 ? 0 : metric.getActiveTasks());
                default -> {}
            }
        }

        metric.setLastUpdated(LocalDateTime.now());
        metricRepository.save(metric);

        log.info("📊 [Kafka] Métrica actualizada — proyecto='{}' status='{}' completion={}%",
                metric.getProjectName(), metric.getProjectStatus(),
                metric.getCompletionPercentage());
    }

    public List<KpiResponse> getGeneralMetrics() {
        return metricRepository.findAll().stream()
                .map(this::toKpiResponse)
                .collect(Collectors.toList());
    }

    public KpiResponse getProjectMetrics(Long projectId) {
        return metricRepository.findByProjectId(projectId)
                .map(this::toKpiResponse)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Métricas no encontradas para el proyecto ID: " + projectId));
    }

    public KpiSummaryResponse getSummary() {
        List<ProjectMetric> all = metricRepository.findAll();

        double avgCompletion = all.stream()
                .mapToDouble(ProjectMetric::getCompletionPercentage)
                .average()
                .orElse(0.0);

        long active = all.stream()
                .filter(m -> "IN_PROGRESS".equals(m.getProjectStatus()))
                .count();

        long completed = all.stream()
                .filter(m -> "COMPLETED".equals(m.getProjectStatus()))
                .count();

        return KpiSummaryResponse.builder()
                .totalProjects(all.size())
                .averageCompletion(Math.round(avgCompletion * 100.0) / 100.0)
                .activeProjects(active)
                .completedProjects(completed)
                .metrics(all.stream().map(this::toKpiResponse).collect(Collectors.toList()))
                .build();
    }

    private KpiResponse toKpiResponse(ProjectMetric m) {
        return KpiResponse.builder()
                .projectId(m.getProjectId())
                .projectStatus(m.getProjectStatus())
                .completionPercentage(m.getCompletionPercentage())
                .activeTasks(m.getActiveTasks())
                .lastUpdated(m.getLastUpdated())
                .build();
    }

    private double computeCompletionPercentage(String status) {
        return switch (status.toUpperCase()) {
            case "PLANNING"    -> 10.0;
            case "IN_PROGRESS" -> 50.0;
            case "ON_HOLD"     -> 35.0;
            case "COMPLETED"   -> 100.0;
            case "CANCELLED"   -> 0.0;
            default            -> 0.0;
        };
    }
}