package com.innovatech.analitica;

import com.innovatech.analitica.controller.AnaliticaController;
import com.innovatech.analitica.dto.KpiResponse;
import com.innovatech.analitica.dto.KpiSummaryResponse;
import com.innovatech.analitica.service.AnaliticaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnaliticaController.class)
@DisplayName("AnaliticaController — WebMvcTest")
class AnaliticaControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean  private AnaliticaService analiticaService;

    private KpiResponse buildKpi(Long id) {
        return KpiResponse.builder()
                .projectId(id).projectStatus("IN_PROGRESS")
                .completionPercentage(50.0).activeTasks(3)
                .lastUpdated(LocalDateTime.now()).build();
    }

    @Test
    @DisplayName("GET /api/v1/analitica/health → 200")
    void health_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/analitica/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/analitica/metricas → 200 con lista")
    void getAllMetrics_returns200WithList() throws Exception {
        when(analiticaService.getGeneralMetrics()).thenReturn(List.of(buildKpi(1L)));

        mockMvc.perform(get("/api/v1/analitica/metricas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].projectId").value(1))
                .andExpect(jsonPath("$[0].projectStatus").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("GET /api/v1/analitica/metricas → 200 con lista vacía")
    void getAllMetrics_emptyList_returns200() throws Exception {
        when(analiticaService.getGeneralMetrics()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/analitica/metricas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/analitica/metricas/proyecto/{id} → 200")
    void getProjectMetrics_existing_returns200() throws Exception {
        when(analiticaService.getProjectMetrics(1L)).thenReturn(buildKpi(1L));

        mockMvc.perform(get("/api/v1/analitica/metricas/proyecto/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(1))
                .andExpect(jsonPath("$.completionPercentage").value(50.0));
    }

    @Test
    @DisplayName("GET /api/v1/analitica/metricas/proyecto/{id} → 400 si no existe")
    void getProjectMetrics_notFound_returns400() throws Exception {
        when(analiticaService.getProjectMetrics(anyLong()))
                .thenThrow(new IllegalArgumentException("Métricas no encontradas para el proyecto ID: 99"));

        mockMvc.perform(get("/api/v1/analitica/metricas/proyecto/99"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/analitica/resumen → 200 con datos calculados")
    void getSummary_returns200() throws Exception {
        KpiSummaryResponse summary = KpiSummaryResponse.builder()
                .totalProjects(2).averageCompletion(75.0)
                .activeProjects(1L).completedProjects(1L)
                .metrics(List.of(buildKpi(1L), buildKpi(2L))).build();
        when(analiticaService.getSummary()).thenReturn(summary);

        mockMvc.perform(get("/api/v1/analitica/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProjects").value(2))
                .andExpect(jsonPath("$.averageCompletion").value(75.0))
                .andExpect(jsonPath("$.activeProjects").value(1));
    }
}