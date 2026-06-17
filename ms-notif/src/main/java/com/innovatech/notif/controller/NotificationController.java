package com.innovatech.notif.controller;

import com.innovatech.notif.model.Notification;
import com.innovatech.notif.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notificaciones")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/proyecto/{projectId}")
    public ResponseEntity<List<Notification>> getNotifications(@PathVariable Long projectId) {
        return ResponseEntity.ok(notificationService.getNotificationsForProject(projectId));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.noContent().build();
    }
}