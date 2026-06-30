package com.innovatech.recursos.repository;

import com.innovatech.recursos.model.Resource;

import java.util.List;
import java.util.Optional;

public interface IResourceRepository {
    Optional<Resource> findById(Long id);
    List<Resource>     findAll();
    List<Resource>     findByAvailable(boolean available);
    List<Resource>     findByDepartment(String department);
    List<Resource>     findByRole(Resource.ResourceRole role);
    List<Resource>     findByAssignedProjectId(Long assignedProjectId);
    boolean            existsByEmail(String email);
    Resource           save(Resource resource);
    void               deleteById(Long id);
    /** NUEVO: busca recurso por el userId de ms-auth para correlación Kafka. */
    Optional<Resource> findByUserId(Long userId);
}