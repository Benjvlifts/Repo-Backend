package com.innovatech.recursos.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Espejo del DTO producido por ms-proyectos. Campos alineados exactamente. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectEventMessage {
    private String eventId;
    private String timestamp;
    private Long   projectId;
    private String projectName;
    private String type;
    private String status;
    private Long   managerId;
    // Campos nuevos
    private String eventType;
    private Long   assignedEmployeeId;
    private String assignedEmployeeName;
}