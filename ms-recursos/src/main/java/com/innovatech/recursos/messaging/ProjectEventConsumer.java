package com.innovatech.recursos.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor Kafka — escucha eventos del topic "innovatech.project.created".
 * Cuando ms-proyectos crea un proyecto, ms-recursos recibe la notificación
 * y puede tomar acciones (ej: marcar recursos como pre-asignados).
 *
 * PATRÓN: Event-Driven Architecture via Message Broker (Kafka).
 * Desacopla completamente ms-proyectos de ms-recursos.
 *
 * @author Benjamin Valdes, Ignacio Munoz
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectEventConsumer {

    @KafkaListener(
        topics = "innovatech.project.created",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeProjectCreated(ProjectEventMessage event) {
        log.info("📨 [Kafka] Evento recibido en ms-recursos: proyecto '{}' (id={}, tipo={})",
            event.getProjectName(), event.getProjectId(), event.getType());

        // Aquí se puede implementar lógica de negocio:
        // - Sugerir recursos disponibles según el tipo de proyecto
        // - Notificar al manager (event.getManagerId())
        // - Registrar el proyecto en el historial de recursos
        log.info("✅ [Kafka] Evento procesado correctamente. Recursos disponibles listos para asignación.");
    }
}