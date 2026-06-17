package com.innovatech.proyectos.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO del evento publicado en Kafka cuando se crea un proyecto.
 * Este mensaje es consumido por ms-recursos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectEventMessage {
    private String eventId;
    private String timestamp;
    private Long projectId;
    private String projectName;
    private String type;
    private String status;
    private Long managerId;
}