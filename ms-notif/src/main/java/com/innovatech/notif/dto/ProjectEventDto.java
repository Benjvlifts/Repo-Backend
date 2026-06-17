package com.innovatech.notif.dto;

import lombok.Data;

@Data
public class ProjectEventDto {
    private Long projectId;
    private String eventType;
    private String status;
}