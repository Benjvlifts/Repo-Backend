package com.innovatech.proyectos.patterns.factory;

import com.innovatech.proyectos.dto.ProjectDtos.CreateProjectRequest;
import com.innovatech.proyectos.model.Project;
import org.springframework.stereotype.Component;

/**
 * Fábrica concreta para proyectos de tipo SOFTWARE.
 *
 * PATRÓN FACTORY METHOD: Implementación concreta de ProjectFactory.
 * Crea instancias de Project con atributos específicos para desarrollo de software:
 * stack tecnológico, URL de repositorio y estructura de sprints.
 *
 * @author Benjamin Valdes, Ignacio Munoz
 */
@Component
public class SoftwareProjectFactory extends ProjectFactory {

    @Override
    public Project createProject(CreateProjectRequest request) {
        Project project = buildBaseProject(request);

        // Atributos específicos de proyectos SOFTWARE
        project.setTechStack(
            request.getTechStack() != null
                ? request.getTechStack()
                : "Node.js / React / PostgreSQL"
        );
        project.setRepositoryUrl(
            request.getRepositoryUrl() != null
                ? request.getRepositoryUrl()
                : "https://github.com/innovatech/" + slugify(request.getName())
        );

        return project;
    }

    private String slugify(String name) {
        return name.toLowerCase()
                   .replaceAll("[^a-z0-9\\s]", "")
                   .replaceAll("\\s+", "-");
    }
}
