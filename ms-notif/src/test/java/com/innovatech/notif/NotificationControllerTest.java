package com.innovatech.notif;

import com.innovatech.notif.controller.NotificationController;
import com.innovatech.notif.model.Notification;
import com.innovatech.notif.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@DisplayName("NotificationController — WebMvcTest")
class NotificationControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean  private NotificationService notificationService;

    private Notification sample;

    @BeforeEach
    void setUp() {
        sample = Notification.builder()
                .id(1L).targetId(10L).targetType("PROJECT")
                .message("Proyecto 10 actualizado").read(false)
                .createdAt(LocalDateTime.now()).build();
    }

    @Test
    @DisplayName("GET /api/v1/notificaciones/health → 200")
    void health_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/notificaciones/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("GET /api/v1/notificaciones/proyecto/{id} → 200 con lista")
    void getNotifications_returns200WithList() throws Exception {
        when(notificationService.getNotificationsForProject(10L)).thenReturn(List.of(sample));

        mockMvc.perform(get("/api/v1/notificaciones/proyecto/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].targetId").value(10))
                .andExpect(jsonPath("$[0].message").value("Proyecto 10 actualizado"));
    }

    @Test
    @DisplayName("GET /api/v1/notificaciones/proyecto/{id} → 200 lista vacía")
    void getNotifications_empty_returns200() throws Exception {
        when(notificationService.getNotificationsForProject(99L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/notificaciones/proyecto/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/notificaciones/proyecto/{id}/no-leidas → 200")
    void getUnread_returns200() throws Exception {
        when(notificationService.getUnreadForProject(10L)).thenReturn(List.of(sample));

        mockMvc.perform(get("/api/v1/notificaciones/proyecto/10/no-leidas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].read").value(false));
    }

    @Test
    @DisplayName("PATCH /api/v1/notificaciones/{id}/read → 204")
    void markAsRead_existing_returns204() throws Exception {
        doNothing().when(notificationService).markAsRead(1L);

        mockMvc.perform(patch("/api/v1/notificaciones/1/read"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PATCH /api/v1/notificaciones/{id}/read → 400 si no existe")
    void markAsRead_notFound_returns400() throws Exception {
        doThrow(new IllegalArgumentException("Notificación no encontrada con id: 99"))
                .when(notificationService).markAsRead(99L);

        mockMvc.perform(patch("/api/v1/notificaciones/99/read"))
                .andExpect(status().isBadRequest());
    }
}