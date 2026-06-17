package com.innovatech.analitica.service;

import com.innovatech.analitica.dto.ProjectEventDto;
import com.innovatech.analitica.model.ProjectMetric;
import com.innovatech.analitica.repository.ProjectMetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnaliticaService {

    private final ProjectMetricRepository metricRepository;

    @Transactional
    public void processProjectEvent(ProjectEventDto event) {
        ProjectMetric metric = metricRepository.findByProjectId(event.getProjectId())
                .orElse(ProjectMetric.builder()
                        .projectId(event.getProjectId())
                        .build());

        metric.setProjectStatus(event.getStatus() != null ? event.getStatus() : metric.getProjectStatus());
        metric.setCompletionPercentage(event.getCompletionPercentage() != null ? event.getCompletionPercentage() : metric.getCompletionPercentage());
        metric.setActiveTasks(event.getActiveTasks() != null ? event.getActiveTasks() : metric.getActiveTasks());
        metric.setLastUpdated(LocalDateTime.now());

        metricRepository.save(metric);
    }

    public List<ProjectMetric> getGeneralMetrics() {
        return metricRepository.findAll();
    }

    public ProjectMetric getProjectMetrics(Long projectId) {
        return metricRepository.findByProjectId(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Métricas no encontradas para el proyecto ID: " + projectId));
    }
}