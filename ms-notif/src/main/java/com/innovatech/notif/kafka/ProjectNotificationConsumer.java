package com.innovatech.notif.kafka;

import com.innovatech.notif.dto.ProjectEventDto;
import com.innovatech.notif.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectNotificationConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "project-events", groupId = "notif-group")
    public void consumeProjectEvent(ProjectEventDto event) {
        log.info("Notificaciones: Generando alerta para proyecto ID: {}", event.getProjectId());
        notificationService.processProjectEvent(event);
    }
}