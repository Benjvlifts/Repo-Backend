package com.innovatech.proyectos.messaging;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectEventProducer — publicación de eventos en Kafka")
class ProjectEventProducerTest {

    private static final String TOPIC = "innovatech.project.created";

    @Mock private KafkaTemplate<String, ProjectEventMessage> kafkaTemplate;

    private ProjectEventProducer producer;

    @BeforeEach
    void setUp() {
        producer = new ProjectEventProducer(kafkaTemplate);

        SendResult<String, ProjectEventMessage> sendResult = mock(SendResult.class);
        RecordMetadata recordMetadata = new RecordMetadata(
                new TopicPartition(TOPIC, 0), 0L, 0, 0L, 0, 0);
        when(sendResult.getRecordMetadata()).thenReturn(recordMetadata);

        when(kafkaTemplate.send(eq(TOPIC), anyString(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(CompletableFuture.completedFuture(sendResult));
    }

    @Test
    @DisplayName("publishProjectCreated envía un evento PROJECT_CREATED con los datos del proyecto")
    void publishProjectCreated_sendsProjectCreatedEvent() {
        producer.publishProjectCreated(1L, "Portal Retail", "SOFTWARE", "PLANNING", 5L);

        ArgumentCaptor<ProjectEventMessage> captor = ArgumentCaptor.forClass(ProjectEventMessage.class);
        verify(kafkaTemplate).send(eq(TOPIC), anyString(), captor.capture());

        ProjectEventMessage event = captor.getValue();
        assertThat(event.getEventType()).isEqualTo("PROJECT_CREATED");
        assertThat(event.getProjectId()).isEqualTo(1L);
        assertThat(event.getProjectName()).isEqualTo("Portal Retail");
        assertThat(event.getType()).isEqualTo("SOFTWARE");
        assertThat(event.getStatus()).isEqualTo("PLANNING");
        assertThat(event.getManagerId()).isEqualTo(5L);
        assertThat(event.getEventId()).isNotBlank();
        assertThat(event.getTimestamp()).isNotBlank();
    }

    @Test
    @DisplayName("publishProjectEvent envía un evento STATUS_CHANGED con los datos del proyecto")
    void publishProjectEvent_sendsStatusChangedEvent() {
        producer.publishProjectEvent(1L, "Portal Retail", "SOFTWARE", "IN_PROGRESS", 5L);

        ArgumentCaptor<ProjectEventMessage> captor = ArgumentCaptor.forClass(ProjectEventMessage.class);
        verify(kafkaTemplate).send(eq(TOPIC), anyString(), captor.capture());

        ProjectEventMessage event = captor.getValue();
        assertThat(event.getEventType()).isEqualTo("STATUS_CHANGED");
        assertThat(event.getStatus()).isEqualTo("IN_PROGRESS");
    }

    @Test
    @DisplayName("publishResourceAssigned envía un evento RESOURCE_ASSIGNED con los datos del empleado")
    void publishResourceAssigned_sendsResourceAssignedEvent() {
        producer.publishResourceAssigned(1L, "Portal Retail", 42L, "Benjamín Valdés");

        ArgumentCaptor<ProjectEventMessage> captor = ArgumentCaptor.forClass(ProjectEventMessage.class);
        verify(kafkaTemplate).send(eq(TOPIC), anyString(), captor.capture());

        ProjectEventMessage event = captor.getValue();
        assertThat(event.getEventType()).isEqualTo("RESOURCE_ASSIGNED");
        assertThat(event.getProjectId()).isEqualTo(1L);
        assertThat(event.getAssignedEmployeeId()).isEqualTo(42L);
        assertThat(event.getAssignedEmployeeName()).isEqualTo("Benjamín Valdés");
        assertThat(event.getType()).isNull();
        assertThat(event.getManagerId()).isNull();
    }

    @Test
    @DisplayName("publishResourceUnassigned envía un evento RESOURCE_UNASSIGNED con los datos del empleado")
    void publishResourceUnassigned_sendsResourceUnassignedEvent() {
        producer.publishResourceUnassigned(1L, "Portal Retail", 42L, "Benjamín Valdés");

        ArgumentCaptor<ProjectEventMessage> captor = ArgumentCaptor.forClass(ProjectEventMessage.class);
        verify(kafkaTemplate).send(eq(TOPIC), anyString(), captor.capture());

        ProjectEventMessage event = captor.getValue();
        assertThat(event.getEventType()).isEqualTo("RESOURCE_UNASSIGNED");
        assertThat(event.getAssignedEmployeeId()).isEqualTo(42L);
        assertThat(event.getAssignedEmployeeName()).isEqualTo("Benjamín Valdés");
    }

    @Test
    @DisplayName("cuando kafkaTemplate.send falla, no propaga la excepción (manejo asíncrono vía whenComplete)")
    void publishProjectCreated_whenSendFails_doesNotThrow() {
        CompletableFuture<SendResult<String, ProjectEventMessage>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka no disponible"));
        when(kafkaTemplate.send(eq(TOPIC), anyString(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(failedFuture);

        producer.publishProjectCreated(1L, "Portal Retail", "SOFTWARE", "PLANNING", 5L);

        verify(kafkaTemplate).send(eq(TOPIC), anyString(), org.mockito.ArgumentMatchers.any());
    }
}