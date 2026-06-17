package com.innovatech.analitica.kafka;

import com.innovatech.analitica.dto.ProjectEventDto;
import com.innovatech.analitica.service.AnaliticaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectEventConsumer {

    private final AnaliticaService analiticaService;

    @KafkaListener(topics = "project-events", groupId = "analitica-group")
    public void consumeProjectEvent(ProjectEventDto event) {
        log.info("Analítica: Recibido evento de proyecto ID: {} - Tipo: {}", event.getProjectId(), event.getEventType());
        analiticaService.processProjectEvent(event);
    }
}