package com.innovatech.notif.service;

import com.innovatech.notif.dto.ProjectEventDto;
import com.innovatech.notif.model.Notification;
import com.innovatech.notif.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
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
        String msg = String.format("Proyecto %d ha registrado un evento: %s. Estado: %s",
                event.getProjectId(), event.getEventType(), event.getStatus());
        createNotification(event.getProjectId(), "PROJECT", msg);
    }

    public List<Notification> getNotificationsForProject(Long projectId) {
        return notificationRepository.findByTargetIdAndTargetTypeOrderByCreatedAtDesc(projectId, "PROJECT");
    }

    public List<Notification> getUnreadForProject(Long projectId) {
        return notificationRepository.findByTargetIdAndTargetTypeAndIsReadFalse(projectId, "PROJECT");
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notificación no encontrada con id: " + notificationId));
        notification.setRead(true);
        notificationRepository.save(notification);
    }
}