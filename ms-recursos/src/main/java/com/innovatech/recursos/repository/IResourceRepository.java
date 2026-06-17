package com.innovatech.recursos.repository;

import com.innovatech.recursos.model.Resource;
import java.util.List;
import java.util.Optional;

/**
 * Interfaz de dominio — Patrón Repository.
 * La lógica de negocio depende de esta abstracción, nunca de JPA directamente.
 */
public interface IResourceRepository {
    Optional<Resource> findById(Long id);
    List<Resource> findAll();
    List<Resource> findByAvailable(boolean available);
    List<Resource> findByDepartment(String department);
    List<Resource> findByRole(Resource.ResourceRole role);
    boolean existsByEmail(String email);
    Resource save(Resource resource);
    void deleteById(Long id);
}