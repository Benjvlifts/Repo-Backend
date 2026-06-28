package com.innovatech.analitica.controller;

import com.innovatech.analitica.dto.KpiResponse;
import com.innovatech.analitica.dto.KpiSummaryResponse;
import com.innovatech.analitica.service.AnaliticaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analitica")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Analítica", description = "Métricas generales y KPIs por proyecto")
public class AnaliticaController {

    private final AnaliticaService analiticaService;

    @GetMapping("/metricas")
    @Operation(summary = "Listar métricas generales", description = "Retorna las métricas de todos los proyectos.")
    @ApiResponse(responseCode = "200", description = "Lista de métricas")
    public ResponseEntity<List<KpiResponse>> getAllMetrics() {
        return ResponseEntity.ok(analiticaService.getGeneralMetrics());
    }

    @GetMapping("/metricas/proyecto/{projectId}")
    @Operation(summary = "Obtener métricas de un proyecto", description = "Retorna las métricas específicas de un proyecto.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Métricas encontradas"),
            @ApiResponse(responseCode = "404", description = "No existen métricas para el proyecto indicado")
    })
    public ResponseEntity<KpiResponse> getProjectMetrics(@PathVariable Long projectId) {
        return ResponseEntity.ok(analiticaService.getProjectMetrics(projectId));
    }

    @GetMapping("/resumen")
    @Operation(summary = "Resumen ejecutivo de KPIs", description = "Retorna un resumen agregado: total de proyectos, promedio de completitud, proyectos activos y completados.")
    @ApiResponse(responseCode = "200", description = "Resumen de KPIs")
    public ResponseEntity<KpiSummaryResponse> getSummary() {
        return ResponseEntity.ok(analiticaService.getSummary());
    }

    @GetMapping("/health")
    @Operation(summary = "Estado del servicio", description = "Endpoint de health check de ms-analitica.")
    @ApiResponse(responseCode = "200", description = "Servicio activo")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("{\"status\":\"UP\",\"service\":\"ms-analitica\"}");
    }
}