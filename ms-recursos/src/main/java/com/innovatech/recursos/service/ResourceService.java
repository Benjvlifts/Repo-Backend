package com.innovatech.recursos.service;

import com.innovatech.recursos.dto.ResourceDtos.*;
import com.innovatech.recursos.model.Resource;
import com.innovatech.recursos.repository.IResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de lógica de negocio para recursos humanos.
 * Patrón Repository: depende de IResourceRepository (interfaz), no de JPA.
 *
 * @author Benjamin Valdes, Ignacio Munoz
 */
@Service
@RequiredArgsConstructor
public class ResourceService {

    private final IResourceRepository resourceRepository;

    @Transactional
    public ResourceResponse createResource(CreateResourceRequest req) {
        if (resourceRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Ya existe un recurso con el email: " + req.getEmail());
        }
        Resource.ResourceRole role = req.getRole() != null ? req.getRole() : Resource.ResourceRole.DEVELOPER;
        Resource resource = Resource.builder()
                .name(req.getName())
                .email(req.getEmail())
                .department(req.getDepartment())
                .role(role)
                .skills(req.getSkills())
                .available(true)
                .build();
        return toResponse(resourceRepository.save(resource));
    }

    public List<ResourceResponse> getAllResources() {
        return resourceRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<ResourceResponse> getAvailableResources() {
        return resourceRepository.findByAvailable(true).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<ResourceResponse> getByDepartment(String department) {
        return resourceRepository.findByDepartment(department).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public ResourceResponse getById(Long id) {
        return toResponse(resourceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recurso no encontrado con id: " + id)));
    }

    @Transactional
    public ResourceResponse updateAvailability(Long id, UpdateAvailabilityRequest req) {
        Resource r = resourceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recurso no encontrado con id: " + id));
        r.setAvailable(req.isAvailable());
        return toResponse(resourceRepository.save(r));
    }

    @Transactional
    public ResourceResponse assignToProject(Long resourceId, Long projectId, String projectName) {
        Resource r = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("Recurso no encontrado"));
        r.setAssignedProjectId(projectId);
        r.setAssignedProjectName(projectName);
        r.setAvailable(false);
        return toResponse(resourceRepository.save(r));
    }

    @Transactional
    public void deleteResource(Long id) {
        if (!resourceRepository.existsByEmail("")) {
            resourceRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Recurso no encontrado con id: " + id));
        }
        resourceRepository.deleteById(id);
    }

    private ResourceResponse toResponse(Resource r) {
        return ResourceResponse.builder()
                .id(r.getId())
                .name(r.getName())
                .email(r.getEmail())
                .department(r.getDepartment())
                .role(r.getRole().name())
                .available(r.isAvailable())
                .skills(r.getSkills())
                .assignedProjectId(r.getAssignedProjectId())
                .assignedProjectName(r.getAssignedProjectName())
                .createdAt(r.getCreatedAt())
                .build();
    }
}