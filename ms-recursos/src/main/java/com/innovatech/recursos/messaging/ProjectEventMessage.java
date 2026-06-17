package com.innovatech.recursos.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO del evento Kafka recibido desde ms-proyectos.
 * Contiene la información del proyecto creado.
 */
@Data
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