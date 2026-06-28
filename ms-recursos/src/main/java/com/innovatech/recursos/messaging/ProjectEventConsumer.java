package com.innovatech.recursos.messaging;

import com.innovatech.recursos.service.ResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * FIX Bug 3: implementa lógica de negocio real.
 * Cuando ms-proyectos publica un evento con status=COMPLETED,
 * este consumidor libera automáticamente los recursos asignados.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectEventConsumer {

    private final ResourceService resourceService;

    @KafkaListener(
        topics = "innovatech.project.created",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeProjectCreated(ProjectEventMessage event) {
        log.info("📨 [Kafka] Evento recibido: proyecto='{}' (id={}, tipo={}, estado={})",
            event.getProjectName(), event.getProjectId(), event.getType(), event.getStatus());

        if ("COMPLETED".equalsIgnoreCase(event.getStatus())) {
            // FIX: libera automáticamente la capacidad cuando un proyecto finaliza
            int released = resourceService.releaseByProject(event.getProjectId());
            log.info("✅ [Kafka] {} recurso(s) liberado(s) — proyecto id={} completado.",
                released, event.getProjectId());
        } else {
            log.info("ℹ️  [Kafka] Evento '{}' procesado. Estado: {}. Sin acción de capacidad requerida.",
                event.getProjectName(), event.getStatus());
        }
    }
}