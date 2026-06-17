package com.innovatech.recursos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Microservicio de Gestión de Recursos Humanos.
 * Puerto: 8083 | Base de datos: innovatech_recursos_db
 *
 * @author Benjamin Valdes, Ignacio Munoz
 */
@SpringBootApplication
public class MsRecursosApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsRecursosApplication.class, args);
    }
}