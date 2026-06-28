package com.innovatech.proyectos.patterns.factory;

import com.innovatech.proyectos.dto.ProjectDtos.CreateProjectRequest;
import com.innovatech.proyectos.model.Project;
import org.springframework.stereotype.Component;

/**
 * Fábrica concreta para proyectos de tipo INFRASTRUCTURE.
 *
 * PATRÓN FACTORY METHOD: Crea proyectos de infraestructura con atributos
 * de proveedor cloud, presupuesto USD y configuraciones de red.
 *
 * @author Benjamin Valdes, Ignacio Munoz
 */
@Component
public class InfrastructureProjectFactory extends ProjectFactory {

    private static final String DEFAULT_CLOUD = "AWS";

    @Override
    public Project createProject(CreateProjectRequest request) {
        Project project = buildBaseProject(request);

        // Atributos específicos de proyectos INFRASTRUCTURE
        project.setCloudProvider(
            request.getCloudProvider() != null
                ? request.getCloudProvider()
                : DEFAULT_CLOUD
        );
        project.setBudgetUsd(
            request.getBudgetUsd() != null
                ? request.getBudgetUsd()
                : 0.0
        );

        return project;
    }
}