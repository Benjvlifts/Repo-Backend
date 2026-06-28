package com.innovatech.analitica.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de OpenAPI/Swagger para ms-analitica.
 * Expone la documentación interactiva en /swagger-ui.html
 * y el contrato JSON en /v3/api-docs.
 *
 * @author Benjamin Valdes, Ignacio Munoz
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Innovatech - ms-analitica API")
                        .description("Microservicio de Analítica y KPIs: métricas generales y métricas por proyecto.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Benjamin Valdes, Ignacio Munoz")));
    }
}
