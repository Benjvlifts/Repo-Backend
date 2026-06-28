package com.innovatech.proyectos.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectEventProducer {

    private static final String TOPIC = "innovatech.project.created";
    private final KafkaTemplate<String, ProjectEventMessage> kafkaTemplate;

    /**
     * Método genérico — usado tanto al crear un proyecto como al cambiar su estado.
     * FIX: permite que ms-recursos libere recursos cuando status=COMPLETED.
     */
    public void publishProjectEvent(Long projectId, String projectName,
                                     String type, String status, Long managerId) {
        ProjectEventMessage event = ProjectEventMessage.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(Instant.now().toString())
                .projectId(projectId).projectName(projectName)
                .type(type).status(status).managerId(managerId)
                .build();

        kafkaTemplate.send(TOPIC, event.getEventId(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("📤 [Kafka] Evento publicado: proyecto='{}' status='{}' offset={}",
                                projectName, status, result.getRecordMetadata().offset());
                    } else {
                        log.error("❌ [Kafka] Error publicando evento: {}", ex.getMessage());
                    }
                });
    }

    /** Mantiene la firma original para no romper llamadas existentes. */
    public void publishProjectCreated(Long projectId, String projectName,
                                       String type, String status, Long managerId) {
        publishProjectEvent(projectId, projectName, type, status, managerId);
    }
}