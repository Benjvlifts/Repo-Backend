package com.innovatech.notif.controller;

import com.innovatech.notif.model.Notification;
import com.innovatech.notif.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notificaciones")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Notificaciones", description = "Consulta y marcado de notificaciones generadas por eventos de proyectos")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/proyecto/{projectId}")
    @Operation(summary = "Listar notificaciones de un proyecto",
               description = "Retorna todas las notificaciones asociadas a un proyecto.")
    // FIX: se agrega 404 faltante
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de notificaciones"),
            @ApiResponse(responseCode = "404", description = "No existen notificaciones para el proyecto indicado")
    })
    public ResponseEntity<List<Notification>> getNotifications(@PathVariable Long projectId) {
        return ResponseEntity.ok(notificationService.getNotificationsForProject(projectId));
    }

    @GetMapping("/proyecto/{projectId}/no-leidas")
    @Operation(summary = "Listar notificaciones no leídas",
               description = "Retorna solo las notificaciones no leídas de un proyecto.")
    // FIX: se agrega 404 faltante
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de notificaciones no leídas"),
            @ApiResponse(responseCode = "404", description = "No existen notificaciones para el proyecto indicado")
    })
    public ResponseEntity<List<Notification>> getUnread(@PathVariable Long projectId) {
        return ResponseEntity.ok(notificationService.getUnreadForProject(projectId));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Marcar notificación como leída", description = "Actualiza el estado de una notificación a leída.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Notificación marcada como leída"),
            @ApiResponse(responseCode = "404", description = "Notificación no encontrada")
    })
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/health")
    @Operation(summary = "Estado del servicio", description = "Health check de ms-notif.")
    @ApiResponse(responseCode = "200", description = "Servicio activo")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "ms-notif", "version", "1.0.0"));
    }
}