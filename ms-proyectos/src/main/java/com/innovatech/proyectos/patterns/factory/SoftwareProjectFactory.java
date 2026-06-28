package com.innovatech.proyectos.patterns.factory;

import com.innovatech.proyectos.dto.ProjectDtos.CreateProjectRequest;
import com.innovatech.proyectos.model.Project;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * FIX Bug 7: reemplaza URL hardcodeada a github.com/innovatech/
 * por propiedad configurable vía ${app.repository.base-url}.
 */
@Component
public class SoftwareProjectFactory extends ProjectFactory {

    // FIX Bug 7: era literal "https://github.com/innovatech/"
    @Value("${app.repository.base-url:https://github.com/innovatech/}")
    private String repositoryBaseUrl;

    @Override
    public Project createProject(CreateProjectRequest request) {
        Project project = buildBaseProject(request);

        project.setTechStack(
            request.getTechStack() != null
                ? request.getTechStack()
                : "Node.js / React / PostgreSQL"
        );
        project.setRepositoryUrl(
            request.getRepositoryUrl() != null
                ? request.getRepositoryUrl()
                : repositoryBaseUrl + slugify(request.getName())
        );
        return project;
    }

    private String slugify(String name) {
        return name.toLowerCase()
                   .replaceAll("[^a-z0-9\\s]", "")
                   .replaceAll("\\s+", "-");
    }
}