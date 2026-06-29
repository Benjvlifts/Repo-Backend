package com.innovatech.notif.dto;

import lombok.Data;

// FIX Mismatch Kafka: campos alineados exactamente con ProjectEventMessage (ms-proyectos)
@Data
public class ProjectEventDto {
    private String eventId;
    private String timestamp;
    private Long projectId;
    private String projectName;
    private String type;
    private String status;
    private Long managerId;
}