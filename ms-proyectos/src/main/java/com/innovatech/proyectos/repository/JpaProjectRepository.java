package com.innovatech.proyectos.repository;

import com.innovatech.proyectos.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface JpaProjectRepository extends JpaRepository<Project, Long>, IProjectRepository {
    @Override
    List<Project> findByStatus(Project.ProjectStatus status);
    @Override
    List<Project> findByType(Project.ProjectType type);
    @Override
    List<Project> findByManagerId(Long managerId);
    @Override
    List<Project> findByAssignedUserId(Long assignedUserId);
}