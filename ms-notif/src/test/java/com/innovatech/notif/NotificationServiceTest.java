package com.innovatech.notif;

import com.innovatech.notif.dto.ProjectEventDto;
import com.innovatech.notif.model.Notification;
import com.innovatech.notif.repository.NotificationRepository;
import com.innovatech.notif.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService — Pruebas Unitarias")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private Notification sampleNotif;

    @BeforeEach
    void setUp() {
        sampleNotif = Notification.builder()
                .id(1L)
                .targetId(100L)
                .targetType("PROJECT")
                .message("Prueba de notificación")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Creación y Procesamiento")
    class CreationTests {

        @Test
        @DisplayName("✅ Procesa evento de Kafka y crea notificación")
        void processProjectEvent_createsNotification() {
            ProjectEventDto event = new ProjectEventDto();
            event.setProjectId(100L);
            event.setEventType("STATUS_UPDATE");
            event.setStatus("COMPLETED");

            when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

            notificationService.processProjectEvent(event);

            verify(notificationRepository, times(1)).save(argThat(notif -> 
                notif.getTargetId().equals(100L) && 
                notif.getMessage().contains("COMPLETED")
            ));
        }
    }

    @Nested
    @DisplayName("Lectura y Actualización")
    class ReadUpdateTests {

        @Test
        @DisplayName("✅ Obtiene notificaciones de un proyecto")
        void getNotificationsForProject_returnsList() {
            when(notificationRepository.findByTargetIdAndTargetTypeOrderByCreatedAtDesc(100L, "PROJECT"))
                    .thenReturn(List.of(sampleNotif));

            List<Notification> results = notificationService.getNotificationsForProject(100L);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getTargetId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("✅ Marca notificación como leída")
        void markAsRead_existingId_updatesStatus() {
            when(notificationRepository.findById(1L)).thenReturn(Optional.of(sampleNotif));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

            notificationService.markAsRead(1L);

            assertThat(sampleNotif.isRead()).isTrue();
            verify(notificationRepository).save(sampleNotif);
        }

        @Test
        @DisplayName("❌ Lanza excepción si la notificación a leer no existe")
        void markAsRead_nonExistingId_throwsException() {
            when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.markAsRead(99L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}