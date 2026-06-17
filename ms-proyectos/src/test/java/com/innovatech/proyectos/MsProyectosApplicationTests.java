package com.innovatech.proyectos;

import com.innovatech.proyectos.messaging.ProjectEventMessage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class MsProyectosApplicationTests {

    @MockBean
    KafkaTemplate<String, ProjectEventMessage> kafkaTemplate;

    @Test
    void contextLoads() {
    }

}