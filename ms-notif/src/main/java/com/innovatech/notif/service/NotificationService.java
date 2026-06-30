package com.innovatech.notif.service;

import com.innovatech.notif.dto.ProjectEventDto;
import com.innovatech.notif.model.Notification;
import com.innovatech.notif.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
@Service
@Slf4j 
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public Notification createNotification(Long targetId, String targetType, String message) {
        Notification notification = Notification.builder()
                .targetId(targetId)
                .targetType(targetType)
                .message(message)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();
        return notificationRepository.save(notification);
    }

    @Transactional
    public void processProjectEvent(ProjectEventDto event) {
        String msg = buildMessage(event);
        createNotification(event.getProjectId(), "PROJECT", msg);
        log.info("🔔 [Kafka] Notificación creada — proyecto id={}: {}", event.getProjectId(), msg);
    }

    public List<Notification> getNotificationsForProject(Long projectId) {
        return notificationRepository.findByTargetIdAndTargetTypeOrderByCreatedAtDesc(projectId, "PROJECT");
    }

    public List<Notification> getUnreadForProject(Long projectId) {
        return notificationRepository.findByTargetIdAndTargetTypeAndReadFalse(projectId, "PROJECT");
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notificación no encontrada con id: " + notificationId));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    private String buildMessage(ProjectEventDto event) {
        String name      = event.getProjectName() != null
                           ? event.getProjectName()
                           : "Proyecto ID " + event.getProjectId();
        String eventType = event.getEventType() != null ? event.getEventType().toUpperCase() : "";
        String status    = event.getStatus() != null ? event.getStatus() : "";

        return switch (eventType) {
            case "PROJECT_CREATED" ->
                String.format("✅ Nuevo proyecto creado: \"%s\" — Estado inicial: %s.", name, status);
            case "STATUS_CHANGED"  ->
                buildStatusMessage(name, status);
            case "RESOURCE_ASSIGNED" -> {
                String emp = event.getAssignedEmployeeName() != null
                             ? event.getAssignedEmployeeName()
                             : "ID " + event.getAssignedEmployeeId();
                yield String.format("👤 %s fue asignado/a al proyecto \"%s\".", emp, name);
            }
            case "RESOURCE_UNASSIGNED" -> {
                String emp = event.getAssignedEmployeeName() != null
                             ? event.getAssignedEmployeeName()
                             : "ID " + event.getAssignedEmployeeId();
                yield String.format("🚪 %s fue removido/a del proyecto \"%s\".", emp, name);
            }
            default ->
                // Fallback por compatibilidad con mensajes sin eventType
                buildStatusMessage(name, status);
        };
    }

    private String buildStatusMessage(String projectName, String status) {
        return switch (status.toUpperCase()) {
            case "IN_PROGRESS" -> String.format("🚀 El proyecto \"%s\" pasó a estado En Progreso.", projectName);
            case "ON_HOLD"     -> String.format("⏸️  El proyecto \"%s\" fue puesto en pausa.", projectName);
            case "COMPLETED"   -> String.format("🎉 El proyecto \"%s\" fue completado exitosamente.", projectName);
            case "CANCELLED"   -> String.format("❌ El proyecto \"%s\" fue cancelado.", projectName);
            case "PLANNING"    -> String.format("📋 El proyecto \"%s\" volvió a estado de Planificación.", projectName);
            default            -> String.format("ℹ️  Proyecto \"%s\" — nuevo estado: %s.", projectName, status);
        };
    }
}