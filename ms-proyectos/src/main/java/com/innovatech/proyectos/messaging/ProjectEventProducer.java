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

    // ── API pública ──────────────────────────────────────────────────────────

    /** Emite PROJECT_CREATED al crear un proyecto. */
    public void publishProjectCreated(Long projectId, String projectName,
                                      String type, String status, Long managerId) {
        sendEvent(buildEvent(projectId, projectName, type, status, managerId,
                  "PROJECT_CREATED", null, null));
    }

    /** Emite STATUS_CHANGED al actualizar estado de un proyecto. */
    public void publishProjectEvent(Long projectId, String projectName,
                                    String type, String status, Long managerId) {
        sendEvent(buildEvent(projectId, projectName, type, status, managerId,
                  "STATUS_CHANGED", null, null));
    }

    /** Emite RESOURCE_ASSIGNED al asignar un empleado a un proyecto. */
    public void publishResourceAssigned(Long projectId, String projectName,
                                        Long employeeId, String employeeName) {
        sendEvent(buildEvent(projectId, projectName, null, null, null,
                  "RESOURCE_ASSIGNED", employeeId, employeeName));
    }

    /** Emite RESOURCE_UNASSIGNED al desasignar un empleado de un proyecto. */
    public void publishResourceUnassigned(Long projectId, String projectName,
                                          Long employeeId, String employeeName) {
        sendEvent(buildEvent(projectId, projectName, null, null, null,
                  "RESOURCE_UNASSIGNED", employeeId, employeeName));
    }

    // ── Helpers privados ─────────────────────────────────────────────────────

    private ProjectEventMessage buildEvent(Long projectId, String projectName,
                                           String type, String status, Long managerId,
                                           String eventType, Long employeeId, String employeeName) {
        return ProjectEventMessage.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(Instant.now().toString())
                .projectId(projectId).projectName(projectName)
                .type(type).status(status).managerId(managerId)
                .eventType(eventType)
                .assignedEmployeeId(employeeId)
                .assignedEmployeeName(employeeName)
                .build();
    }

    private void sendEvent(ProjectEventMessage event) {
        kafkaTemplate.send(TOPIC, event.getEventId(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("📤 [Kafka] '{}' publicado: proyecto='{}' offset={}",
                                event.getEventType(), event.getProjectName(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("❌ [Kafka] Error publicando '{}': {}",
                                event.getEventType(), ex.getMessage());
                    }
                });
    }
}