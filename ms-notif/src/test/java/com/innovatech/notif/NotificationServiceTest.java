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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService — Pruebas Unitarias")
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @InjectMocks private NotificationService notificationService;

    private Notification sampleNotification;

    @BeforeEach
    void setUp() {
        sampleNotification = Notification.builder()
                .id(1L).targetId(10L).targetType("PROJECT")
                .message("Proyecto 10 creado").read(false)
                .createdAt(LocalDateTime.now()).build();
    }

    @Nested
    @DisplayName("createNotification()")
    class CreateNotificationTests {

        @Test
        @DisplayName("✅ Crea y persiste notificación")
        void createNotification_savesAndReturns() {
            when(notificationRepository.save(any())).thenReturn(sampleNotification);
            Notification result = notificationService.createNotification(10L, "PROJECT", "Mensaje test");
            assertThat(result.getTargetId()).isEqualTo(10L);
            assertThat(result.getRead()).isFalse(); // FIX: entidad ya no expone isRead(), getter Lombok es getRead()
            verify(notificationRepository).save(any(Notification.class));
        }
    }

    @Nested
    @DisplayName("processProjectEvent()")
    class ProcessEventTests {

        @Test
        @DisplayName("✅ Genera notificación a partir de evento Kafka")
        void processProjectEvent_createsNotification() {
            ProjectEventDto event = new ProjectEventDto();
            event.setProjectId(10L);
            event.setType("PROJECT_CREATED"); // FIX: setEventType → setType (campo en DTO es 'type')
            event.setStatus("PLANNING");

            when(notificationRepository.save(any())).thenReturn(sampleNotification);
            notificationService.processProjectEvent(event);
            verify(notificationRepository).save(any(Notification.class));
        }
    }

    @Nested
    @DisplayName("getNotificationsForProject()")
    class GetNotificationsTests {

        @Test
        @DisplayName("✅ Retorna notificaciones del proyecto")
        void getNotificationsForProject_returnsOrdered() {
            when(notificationRepository.findByTargetIdAndTargetTypeOrderByCreatedAtDesc(10L, "PROJECT"))
                    .thenReturn(List.of(sampleNotification));
            List<Notification> result = notificationService.getNotificationsForProject(10L);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTargetId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("✅ Retorna notificaciones no leídas")
        void getUnreadForProject_returnsUnread() {
            when(notificationRepository.findByTargetIdAndTargetTypeAndReadFalse(10L, "PROJECT"))
                    .thenReturn(List.of(sampleNotification));
            assertThat(notificationService.getUnreadForProject(10L)).hasSize(1);
        }
    }

    @Nested
    @DisplayName("markAsRead()")
    class MarkAsReadTests {

        @Test
        @DisplayName("✅ Marca notificación como leída")
        void markAsRead_existingId_setsReadTrue() {
            when(notificationRepository.findById(1L)).thenReturn(Optional.of(sampleNotification));
            when(notificationRepository.save(any())).thenReturn(sampleNotification);
            notificationService.markAsRead(1L);
            verify(notificationRepository).save(argThat(Notification::getRead)); // FIX: Notification::isRead -> Notification::getRead
        }


        @Test
        @DisplayName("❌ Lanza excepción si notificación no existe")
        void markAsRead_nonExisting_throwsException() {
            when(notificationRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> notificationService.markAsRead(99L))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("99");
        }
    }
}