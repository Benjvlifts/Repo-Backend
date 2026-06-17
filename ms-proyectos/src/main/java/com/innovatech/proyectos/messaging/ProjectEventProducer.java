package com.innovatech.proyectos.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Productor Kafka — publica eventos cuando se crean proyectos.
 * ms-recursos y otros microservicios pueden suscribirse para reaccionar.
 *
 * PATRÓN: Event-Driven Architecture via Message Broker (Kafka).
 * ms-proyectos no conoce ni depende de ms-recursos → desacoplamiento total.
 *
 * @author Benjamin Valdes, Ignacio Munoz
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectEventProducer {

    private static final String TOPIC = "innovatech.project.created";
    private final KafkaTemplate<String, ProjectEventMessage> kafkaTemplate;

    public void publishProjectCreated(Long projectId, String projectName,
                                       String type, String status, Long managerId) {
        ProjectEventMessage event = ProjectEventMessage.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(Instant.now().toString())
                .projectId(projectId)
                .projectName(projectName)
                .type(type)
                .status(status)
                .managerId(managerId)
                .build();

        kafkaTemplate.send(TOPIC, event.getEventId(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("📤 [Kafka] Evento publicado: proyecto='{}' id={} offset={}",
                                projectName, projectId,
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("❌ [Kafka] Error al publicar evento: {}", ex.getMessage());
                    }
                });
    }
}