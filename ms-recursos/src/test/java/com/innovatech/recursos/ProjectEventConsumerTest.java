package com.innovatech.recursos;

import com.innovatech.recursos.messaging.ProjectEventConsumer;
import com.innovatech.recursos.messaging.ProjectEventMessage;
import com.innovatech.recursos.service.ResourceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectEventConsumer (ms-recursos) — Pruebas Unitarias")
class ProjectEventConsumerTest {

    @Mock    private ResourceService resourceService;
    @InjectMocks private ProjectEventConsumer consumer;

    private ProjectEventMessage buildEvent(Long id, String name, String status) {
        ProjectEventMessage e = new ProjectEventMessage();
        e.setProjectId(id);
        e.setProjectName(name);
        e.setType("SOFTWARE");
        e.setStatus(status);
        return e;
    }

    @Test
    @DisplayName("✅ Proyecto COMPLETED → libera recursos automáticamente")
    void consumeProjectCreated_completedStatus_releasesResources() {
        ProjectEventMessage event = buildEvent(1L, "Portal Fintech", "COMPLETED");
        when(resourceService.releaseByProject(1L)).thenReturn(3);

        consumer.consumeProjectCreated(event);

        verify(resourceService, times(1)).releaseByProject(1L);
    }

    @Test
    @DisplayName("✅ Proyecto IN_PROGRESS → NO libera recursos")
    void consumeProjectCreated_inProgressStatus_doesNotRelease() {
        ProjectEventMessage event = buildEvent(2L, "App Móvil", "IN_PROGRESS");

        consumer.consumeProjectCreated(event);

        verifyNoInteractions(resourceService);
    }

    @Test
    @DisplayName("✅ Proyecto PLANNING → NO libera recursos")
    void consumeProjectCreated_planningStatus_doesNotRelease() {
        ProjectEventMessage event = buildEvent(3L, "Infraestructura", "PLANNING");

        consumer.consumeProjectCreated(event);

        verifyNoInteractions(resourceService);
    }

    @Test
    @DisplayName("✅ Status en minúsculas 'completed' → libera recursos (case-insensitive)")
    void consumeProjectCreated_completedLowerCase_releasesResources() {
        ProjectEventMessage event = buildEvent(4L, "ERP System", "completed");
        when(resourceService.releaseByProject(4L)).thenReturn(1);

        consumer.consumeProjectCreated(event);

        verify(resourceService).releaseByProject(4L);
    }
}