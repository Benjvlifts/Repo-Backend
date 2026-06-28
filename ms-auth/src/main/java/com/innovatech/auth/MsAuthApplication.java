package com.innovatech.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del microservicio de Autenticación (ms-auth).
 * Responsable de la gestión de usuarios, tokens JWT y roles.
 *
 * @author Benjamin Valdes, Ignacio Munoz
 * @version 1.0.0
 */
@SpringBootApplication
public class MsAuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsAuthApplication.class, args);
    }
}