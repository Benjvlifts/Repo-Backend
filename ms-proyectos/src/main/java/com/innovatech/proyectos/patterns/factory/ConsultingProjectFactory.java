package com.innovatech.proyectos.patterns.factory;

import com.innovatech.proyectos.dto.ProjectDtos.CreateProjectRequest;
import com.innovatech.proyectos.model.Project;
import org.springframework.stereotype.Component;

/**
 * Fábrica concreta para proyectos de tipo CONSULTING.
 *
 * PATRÓN FACTORY METHOD: Crea proyectos de consultoría con atributos
 * de SLA, nombre del cliente e hitos de entregables.
 *
 * @author Benjamin Valdes, Ignacio Munoz
 */
@Component
public class ConsultingProjectFactory extends ProjectFactory {

    private static final int DEFAULT_SLA_DAYS = 30;

    @Override
    public Project createProject(CreateProjectRequest request) {
        Project project = buildBaseProject(request);

        // Atributos específicos de proyectos CONSULTING
        project.setClientName(request.getClientName());
        project.setSlaDays(
            request.getSlaDays() != null
                ? request.getSlaDays()
                : DEFAULT_SLA_DAYS
        );

        return project;
    }
}
