package com.innovatech.analitica.service;

import com.innovatech.analitica.dto.KpiResponse;
import com.innovatech.analitica.dto.KpiSummaryResponse;
import com.innovatech.analitica.dto.ProjectEventDto;
import com.innovatech.analitica.model.ProjectMetric;
import com.innovatech.analitica.repository.ProjectMetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnaliticaService {

    private final ProjectMetricRepository metricRepository;

    @Transactional
    public void processProjectEvent(ProjectEventDto event) {
        ProjectMetric metric = metricRepository.findByProjectId(event.getProjectId())
                .orElse(ProjectMetric.builder()
                        .projectId(event.getProjectId())
                        .completionPercentage(0.0)
                        .activeTasks(0)
                        .build());

        if (event.getStatus() != null) {
            metric.setProjectStatus(event.getStatus());
        }
        metric.setLastUpdated(LocalDateTime.now());

        metricRepository.save(metric);
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
}