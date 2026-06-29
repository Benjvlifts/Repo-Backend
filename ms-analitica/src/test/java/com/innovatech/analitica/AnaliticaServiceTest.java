package com.innovatech.analitica;

import com.innovatech.analitica.dto.KpiResponse;
import com.innovatech.analitica.dto.KpiSummaryResponse;
import com.innovatech.analitica.dto.ProjectEventDto;
import com.innovatech.analitica.model.ProjectMetric;
import com.innovatech.analitica.repository.ProjectMetricRepository;
import com.innovatech.analitica.service.AnaliticaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnaliticaService — Pruebas Unitarias")
class AnaliticaServiceTest {

    @Mock private ProjectMetricRepository metricRepository;
    @InjectMocks private AnaliticaService analiticaService;

    private ProjectMetric sampleMetric;

    @BeforeEach
    void setUp() {
        sampleMetric = ProjectMetric.builder()
                .id(1L).projectId(10L).projectStatus("IN_PROGRESS")
                .completionPercentage(75.0).activeTasks(5)
                .lastUpdated(LocalDateTime.now()).build();
    }

    @Nested
    @DisplayName("processProjectEvent()")
    class ProcessEventTests {

        @Test
        @DisplayName("✅ Crea métrica nueva si no existe para el proyecto")
        void processEvent_noExistingMetric_createsNew() {
            ProjectEventDto event = new ProjectEventDto();
            event.setProjectId(99L);
            event.setStatus("PLANNING");
            // FIX: setCompletionPercentage y setActiveTasks eliminados —
            //      estos campos ya no existen en ProjectEventDto (alineado con Kafka)

            when(metricRepository.findByProjectId(99L)).thenReturn(Optional.empty());
            when(metricRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            analiticaService.processProjectEvent(event);
            verify(metricRepository).save(any(ProjectMetric.class));
        }

        @Test
        @DisplayName("✅ Actualiza métrica existente")
        void processEvent_existingMetric_updatesFields() {
            ProjectEventDto event = new ProjectEventDto();
            event.setProjectId(10L);
            event.setStatus("COMPLETED");
            // FIX: setCompletionPercentage(100.0) y setActiveTasks(0) eliminados

            when(metricRepository.findByProjectId(10L)).thenReturn(Optional.of(sampleMetric));
            when(metricRepository.save(any())).thenReturn(sampleMetric);

            analiticaService.processProjectEvent(event);
            verify(metricRepository).save(argThat(m -> "COMPLETED".equals(m.getProjectStatus())));
        }

        @Test
        @DisplayName("✅ No sobreescribe campos null del evento")
        void processEvent_nullFields_keepsExistingValues() {
            ProjectEventDto event = new ProjectEventDto();
            event.setProjectId(10L);

            when(metricRepository.findByProjectId(10L)).thenReturn(Optional.of(sampleMetric));
            when(metricRepository.save(any())).thenReturn(sampleMetric);

            analiticaService.processProjectEvent(event);
            verify(metricRepository).save(argThat(m -> "IN_PROGRESS".equals(m.getProjectStatus())));
        }
    }

    @Nested
    @DisplayName("getGeneralMetrics()")
    class GetMetricsTests {

        @Test
        @DisplayName("✅ Retorna lista de KpiResponse")
        void getGeneralMetrics_returnsMappedList() {
            when(metricRepository.findAll()).thenReturn(List.of(sampleMetric));
            List<KpiResponse> result = analiticaService.getGeneralMetrics();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getProjectId()).isEqualTo(10L);
            assertThat(result.get(0).getCompletionPercentage()).isEqualTo(75.0);
        }

        @Test
        @DisplayName("✅ Retorna lista vacía si no hay métricas")
        void getGeneralMetrics_noMetrics_returnsEmpty() {
            when(metricRepository.findAll()).thenReturn(List.of());
            assertThat(analiticaService.getGeneralMetrics()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getProjectMetrics()")
    class GetProjectMetricsTests {

        @Test
        @DisplayName("✅ Retorna métricas del proyecto solicitado")
        void getProjectMetrics_existing_returnsKpiResponse() {
            when(metricRepository.findByProjectId(10L)).thenReturn(Optional.of(sampleMetric));
            KpiResponse response = analiticaService.getProjectMetrics(10L);
            assertThat(response.getProjectId()).isEqualTo(10L);
            assertThat(response.getActiveTasks()).isEqualTo(5);
        }

        @Test
        @DisplayName("❌ Lanza excepción si no hay métricas para el proyecto")
        void getProjectMetrics_nonExisting_throwsException() {
            when(metricRepository.findByProjectId(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> analiticaService.getProjectMetrics(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("getSummary()")
    class GetSummaryTests {

        @Test
        @DisplayName("✅ Calcula promedio de completitud correctamente")
        void getSummary_multipleMetrics_calculatesAverage() {
            ProjectMetric m2 = ProjectMetric.builder().id(2L).projectId(20L)
                    .projectStatus("COMPLETED").completionPercentage(100.0)
                    .activeTasks(0).lastUpdated(LocalDateTime.now()).build();

            when(metricRepository.findAll()).thenReturn(List.of(sampleMetric, m2));
            KpiSummaryResponse summary = analiticaService.getSummary();

            // FIX: KpiSummaryResponse.totalProjects es int, no long -> literal sin sufijo L
            assertThat(summary.getTotalProjects()).isEqualTo(2);
            assertThat(summary.getAverageCompletion()).isEqualTo(87.5);
            assertThat(summary.getActiveProjects()).isEqualTo(1L);
            assertThat(summary.getCompletedProjects()).isEqualTo(1L);
        }

        @Test
        @DisplayName("✅ Retorna ceros si no hay métricas")
        void getSummary_noMetrics_returnsZeros() {
            when(metricRepository.findAll()).thenReturn(List.of());
            KpiSummaryResponse summary = analiticaService.getSummary();

            // FIX: KpiSummaryResponse.totalProjects es int, no long -> literal sin sufijo L
            assertThat(summary.getTotalProjects()).isEqualTo(0);
            assertThat(summary.getAverageCompletion()).isEqualTo(0.0);
        }
    }
}