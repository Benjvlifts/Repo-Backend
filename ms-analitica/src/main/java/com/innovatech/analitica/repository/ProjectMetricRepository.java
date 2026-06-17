package com.innovatech.analitica.repository;

import com.innovatech.analitica.model.ProjectMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectMetricRepository extends JpaRepository<ProjectMetric, Long> {
    Optional<ProjectMetric> findByProjectId(Long projectId);
}