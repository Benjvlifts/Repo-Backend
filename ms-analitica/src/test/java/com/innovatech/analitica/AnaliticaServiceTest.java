package com.innovatech.analitica;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnaliticaService — Pruebas Unitarias")
class AnaliticaServiceTest {

    @Mock
    private ProjectMetricRepository metricRepository;

    @InjectMocks
    private AnaliticaService analiticaService;

    private ProjectMetric sampleMetric;

    @BeforeEach
    void setUp() {
        sampleMetric = ProjectMetric.builder()
                .id(1L)
                .projectId(100L)
                .projectStatus("IN_PROGRESS")
                .completionPercentage(50.0)
                .activeTasks(5)
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Procesamiento de Eventos (Kafka)")
    class ProcessEventTests {

        @Test
        @DisplayName("✅ Crea nueva métrica si el proyecto no existe")
        void processProjectEvent_newProject_savesNewMetric() {
            ProjectEventDto event = new ProjectEventDto();
            event.setProjectId(200L);
            event.setStatus("CREATED");
            event.setCompletionPercentage(0.0);
            event.setActiveTasks(0);

            when(metricRepository.findByProjectId(200L)).thenReturn(Optional.empty());
            when(metricRepository.save(any(ProjectMetric.class))).thenAnswer(i -> i.getArgument(0));

            analiticaService.processProjectEvent(event);

            verify(metricRepository).save(argThat(metric -> 
                metric.getProjectId().equals(200L) && 
                metric.getProjectStatus().equals("CREATED")
            ));
        }

        @Test
        @DisplayName("✅ Actualiza métrica si el proyecto ya existe")
        void processProjectEvent_existingProject_updatesMetric() {
            ProjectEventDto event = new ProjectEventDto();
            event.setProjectId(100L);
            event.setCompletionPercentage(75.0); // Avance
            event.setActiveTasks(3); // Tareas bajaron

            when(metricRepository.findByProjectId(100L)).thenReturn(Optional.of(sampleMetric));
            when(metricRepository.save(any(ProjectMetric.class))).thenAnswer(i -> i.getArgument(0));

            analiticaService.processProjectEvent(event);

            verify(metricRepository).save(argThat(metric -> 
                metric.getCompletionPercentage().equals(75.0) &&
                metric.getActiveTasks().equals(3) &&
                metric.getProjectStatus().equals("IN_PROGRESS") // No se pisó porque era null en el evento
            ));
        }
    }

    @Nested
    @DisplayName("Consultas de Métricas")
    class QueryTests {

        @Test
        @DisplayName("✅ Retorna todas las métricas")
        void getGeneralMetrics_returnsList() {
            when(metricRepository.findAll()).thenReturn(List.of(sampleMetric));

            List<ProjectMetric> results = analiticaService.getGeneralMetrics();

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getProjectId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("✅ Retorna métrica por ID de proyecto")
        void getProjectMetrics_existingId_returnsMetric() {
            when(metricRepository.findByProjectId(100L)).thenReturn(Optional.of(sampleMetric));

            ProjectMetric result = analiticaService.getProjectMetrics(100L);

            assertThat(result.getProjectId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("❌ Lanza excepción si la métrica no existe")
        void getProjectMetrics_nonExistingId_throwsException() {
            when(metricRepository.findByProjectId(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> analiticaService.getProjectMetrics(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Métricas no encontradas");
        }
    }
}