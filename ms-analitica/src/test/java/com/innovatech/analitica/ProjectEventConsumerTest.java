package com.innovatech.analitica;

import com.innovatech.analitica.dto.ProjectEventDto;
import com.innovatech.analitica.kafka.ProjectEventConsumer;
import com.innovatech.analitica.service.AnaliticaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectEventConsumer (ms-analitica) — Pruebas Unitarias")
class ProjectEventConsumerTest {

    @Mock    private AnaliticaService analiticaService;
    @InjectMocks private ProjectEventConsumer consumer;

    @Test
    @DisplayName("✅ Delega el evento recibido a AnaliticaService")
    void consumeProjectEvent_delegatesToService() {
        ProjectEventDto event = new ProjectEventDto();
        event.setProjectId(1L);
        event.setProjectName("Portal Fintech");
        event.setType("SOFTWARE");
        event.setStatus("IN_PROGRESS");

        consumer.consumeProjectEvent(event);

        verify(analiticaService, times(1)).processProjectEvent(event);
    }

    @Test
    @DisplayName("✅ Delega evento COMPLETED a AnaliticaService")
    void consumeProjectEvent_completedStatus_delegatesToService() {
        ProjectEventDto event = new ProjectEventDto();
        event.setProjectId(2L);
        event.setProjectName("Infraestructura Cloud");
        event.setType("INFRASTRUCTURE");
        event.setStatus("COMPLETED");

        consumer.consumeProjectEvent(event);

        verify(analiticaService).processProjectEvent(event);
        verifyNoMoreInteractions(analiticaService);
    }

    @Test
    @DisplayName("✅ Procesa evento con projectId nulo sin lanzar excepción del consumer")
    void consumeProjectEvent_nullProjectId_delegatesAndLetsServiceDecide() {
        ProjectEventDto event = new ProjectEventDto();
        event.setStatus("PLANNING");

        doNothing().when(analiticaService).processProjectEvent(any());
        consumer.consumeProjectEvent(event);

        verify(analiticaService).processProjectEvent(event);
    }
}