package com.innovatech.proyectos.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectEventMessage {
    private String eventId;
    private String timestamp;
    private Long   projectId;
    private String projectName;
    private String type;       // Tipo de proyecto: SOFTWARE | CONSULTING | INFRASTRUCTURE
    private String status;     // Estado: PLANNING | IN_PROGRESS | ON_HOLD | COMPLETED | CANCELLED
    private Long   managerId;

    // ── Campos nuevos para enrutamiento y recursos ──────────────────────────
    /** Discriminador del evento: PROJECT_CREATED | STATUS_CHANGED | RESOURCE_ASSIGNED | RESOURCE_UNASSIGNED */
    private String eventType;
    /** ID del empleado en ms-auth; solo presente en eventos RESOURCE_* */
    private Long   assignedEmployeeId;
    /** Nombre del empleado; solo presente en eventos RESOURCE_* */
    private String assignedEmployeeName;
}