package com.innovatech.notif.dto;

import lombok.Data;

/** Espejo del ProjectEventMessage de ms-proyectos. Campos alineados exactamente. */
@Data
public class ProjectEventDto {
    private String eventId;
    private String timestamp;
    private Long   projectId;
    private String projectName;
    private String type;        // Tipo de proyecto: SOFTWARE | CONSULTING | INFRASTRUCTURE
    private String status;
    private Long   managerId;
    /** NUEVO: discriminador del evento para generar mensajes contextuales. */
    private String eventType;
    private Long   assignedEmployeeId;
    private String assignedEmployeeName;
}