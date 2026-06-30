package com.innovatech.recursos.messaging;

import com.innovatech.recursos.service.ResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectEventConsumer {

    private final ResourceService resourceService;

    @KafkaListener(
        topics   = "innovatech.project.created",
        groupId  = "${spring.kafka.consumer.group-id}"
    )
    public void consumeProjectEvent(ProjectEventMessage event) {
        String eventType = event.getEventType();
        log.info("📨 [Kafka] Evento '{}' recibido — proyecto='{}' (id={})",
                eventType, event.getProjectName(), event.getProjectId());

        if (eventType == null) {
            // Compatibilidad con mensajes sin eventType (legado): revisar solo por status
            if ("COMPLETED".equalsIgnoreCase(event.getStatus())) {
                liberarPorProyecto(event.getProjectId());
            }
            return;
        }

        switch (eventType.toUpperCase()) {
            case "RESOURCE_ASSIGNED" -> {
                if (event.getAssignedEmployeeId() != null) {
                    resourceService.assignToProjectByUserId(
                            event.getAssignedEmployeeId(),
                            event.getProjectId(),
                            event.getProjectName());
                }
            }
            case "RESOURCE_UNASSIGNED" -> {
                if (event.getAssignedEmployeeId() != null) {
                    resourceService.releaseByUserId(event.getAssignedEmployeeId());
                }
            }
            case "STATUS_CHANGED" -> {
                if ("COMPLETED".equalsIgnoreCase(event.getStatus()) ||
                    "CANCELLED".equalsIgnoreCase(event.getStatus())) {
                    liberarPorProyecto(event.getProjectId());
                }
            }
            default -> log.debug("ℹ️  [Kafka] Evento '{}' ignorado en ms-recursos.", eventType);
        }
    }

    private void liberarPorProyecto(Long projectId) {
        int released = resourceService.releaseByProject(projectId);
        log.info("✅ [Kafka] {} recurso(s) liberado(s) — proyecto id={}", released, projectId);
    }
}