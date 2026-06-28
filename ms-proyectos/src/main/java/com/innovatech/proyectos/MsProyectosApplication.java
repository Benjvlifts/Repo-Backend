package com.innovatech.proyectos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del microservicio de Gestión de Proyectos (ms-proyectos).
 * Aplica patrones: Repository Pattern, Factory Method, Circuit Breaker.
 *
 * @author Benjamin Valdes, Ignacio Munoz
 * @version 1.0.0
 */
@SpringBootApplication
public class MsProyectosApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsProyectosApplication.class, args);
    }
}