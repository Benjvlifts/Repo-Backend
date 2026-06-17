package com.innovatech.analitica.controller;

import com.innovatech.analitica.model.ProjectMetric;
import com.innovatech.analitica.service.AnaliticaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analitica")
@RequiredArgsConstructor
public class AnaliticaController {

    private final AnaliticaService analiticaService;

    @GetMapping("/metricas")
    public ResponseEntity<List<ProjectMetric>> getAllMetrics() {
        return ResponseEntity.ok(analiticaService.getGeneralMetrics());
    }

    @GetMapping("/metricas/proyecto/{projectId}")
    public ResponseEntity<ProjectMetric> getProjectMetrics(@PathVariable Long projectId) {
        return ResponseEntity.ok(analiticaService.getProjectMetrics(projectId));
    }
}