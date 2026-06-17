package com.innovatech.proyectos.repository;

import com.innovatech.proyectos.model.Project;
import java.util.List;
import java.util.Optional;

public interface IProjectRepository {
    Optional<Project> findById(Long id);
    List<Project> findAll();
    List<Project> findByStatus(Project.ProjectStatus status);
    List<Project> findByType(Project.ProjectType type);
    List<Project> findByManagerId(Long managerId);
    Project save(Project project);
    void deleteById(Long id);
    boolean existsById(Long id);
    long count();
}