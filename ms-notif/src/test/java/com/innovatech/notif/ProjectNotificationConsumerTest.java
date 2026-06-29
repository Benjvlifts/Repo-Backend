package com.innovatech.notif;

import com.innovatech.notif.dto.ProjectEventDto;
import com.innovatech.notif.kafka.ProjectNotificationConsumer;
import com.innovatech.notif.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectNotificationConsumer (ms-notif) — Pruebas Unitarias")
class ProjectNotificationConsumerTest {

    @Mock    private NotificationService notificationService;
    @InjectMocks private ProjectNotificationConsumer consumer;

    @Test
    @DisplayName("✅ Delega el evento Kafka a NotificationService")
    void consumeProjectEvent_delegatesToService() {
        ProjectEventDto event = new ProjectEventDto();
        event.setProjectId(10L);
        event.setProjectName("Portal Retail");
        event.setType("SOFTWARE");
        event.setStatus("PLANNING");

        consumer.consumeProjectEvent(event);

        verify(notificationService, times(1)).processProjectEvent(event);
    }

    @Test
    @DisplayName("✅ Delega evento COMPLETED a NotificationService")
    void consumeProjectEvent_completedStatus_delegatesToService() {
        ProjectEventDto event = new ProjectEventDto();
        event.setProjectId(5L);
        event.setProjectName("Sistema ERP");
        event.setType("CONSULTING");
        event.setStatus("COMPLETED");

        consumer.consumeProjectEvent(event);

        verify(notificationService).processProjectEvent(event);
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    @DisplayName("✅ Procesa multiples eventos en secuencia")
    void consumeProjectEvent_multipleEvents_allDelegated() {
        ProjectEventDto e1 = new ProjectEventDto();
        e1.setProjectId(1L);
        e1.setStatus("IN_PROGRESS");

        ProjectEventDto e2 = new ProjectEventDto();
        e2.setProjectId(2L);
        e2.setStatus("ON_HOLD");

        consumer.consumeProjectEvent(e1);
        consumer.consumeProjectEvent(e2);

        verify(notificationService).processProjectEvent(e1);
        verify(notificationService).processProjectEvent(e2);
    }
}