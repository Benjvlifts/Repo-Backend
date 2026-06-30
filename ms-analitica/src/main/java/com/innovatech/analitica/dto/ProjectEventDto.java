package com.innovatech.analitica.dto;

import lombok.Data;

/** Espejo del ProjectEventMessage de ms-proyectos. Campos alineados exactamente. */
@Data
public class ProjectEventDto {
    private String eventId;
    private String timestamp;
    private Long   projectId;
    private String projectName;
    private String type;        // Tipo de proyecto: SOFTWARE | CONSULTING | INFRASTRUCTURE
    private String status;      // Estado del proyecto
    private Long   managerId;
    /** NUEVO: discriminador del evento para lógica de negocio en consumidores. */
    private String eventType;   // PROJECT_CREATED | STATUS_CHANGED | RESOURCE_ASSIGNED | RESOURCE_UNASSIGNED
    private Long   assignedEmployeeId;
    private String assignedEmployeeName;
}