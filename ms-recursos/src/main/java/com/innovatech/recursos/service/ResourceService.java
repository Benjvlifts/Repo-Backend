package com.innovatech.recursos.service;

import com.innovatech.recursos.dto.ResourceDtos.*;
import com.innovatech.recursos.model.Resource;
import com.innovatech.recursos.repository.IResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;


@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceService {

    private final IResourceRepository resourceRepository;

    // ... métodos existentes sin cambios ...

        @Transactional
    public ResourceResponse createResource(CreateResourceRequest req) {
        if (resourceRepository.existsByEmail(req.getEmail()))
            throw new IllegalArgumentException("Ya existe un recurso con el email: " + req.getEmail());
        Resource.ResourceRole role = req.getRole() != null ? req.getRole() : Resource.ResourceRole.DEVELOPER;
        Resource resource = Resource.builder()
                .name(req.getName()).email(req.getEmail()).department(req.getDepartment())
                .role(role).skills(req.getSkills()).available(true)
                .userId(req.getUserId())  // NUEVO: vincula con ms-auth
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

    public ResourceResponse getById(Long id) { return toResponse(findOrThrow(id)); }


    @Transactional
    public void assignToProjectByUserId(Long userId, Long projectId, String projectName) {
        resourceRepository.findByUserId(userId).ifPresentOrElse(r -> {
            r.setAvailable(false);
            r.setAssignedProjectId(projectId);
            r.setAssignedProjectName(projectName);
            resourceRepository.save(r);
            log.info("✅ [Kafka] Recurso id={} (userId={}) → no disponible. Proyecto id={}",
                    r.getId(), userId, projectId);
        }, () -> log.warn("⚠️  [Kafka] No se encontró Resource con userId={}. " +
                "Asegúrate de que el recurso fue creado con userId vinculado.", userId));
    }

    @Transactional
    public void releaseByUserId(Long userId) {
        resourceRepository.findByUserId(userId).ifPresent(r -> {
            r.setAvailable(true);
            r.setAssignedProjectId(null);
            r.setAssignedProjectName(null);
            resourceRepository.save(r);
            log.info("✅ [Kafka] Recurso id={} (userId={}) → disponible.", r.getId(), userId);
        });
    }

    @Transactional
    public ResourceResponse updateAvailability(Long id, UpdateAvailabilityRequest req) {
        Resource r = findOrThrow(id);
        r.setAvailable(req.isAvailable());
        return toResponse(resourceRepository.save(r));
    }

    @Transactional
    public ResourceResponse assignToProject(Long resourceId, Long projectId, String projectName) {
        Resource r = findOrThrow(resourceId);
        r.setAssignedProjectId(projectId);
        r.setAssignedProjectName(projectName);
        r.setAvailable(false);
        return toResponse(resourceRepository.save(r));
    }

    /**
     * FIX Bug 3 + assignToProject: libera TODOS los recursos de un proyecto.
     * Llamado desde el consumidor Kafka cuando status=COMPLETED,
     * y desde el endpoint PUT /api/resources/capacity para invocación manual.
     * @return cantidad de recursos liberados
     */
    @Transactional
    public int releaseByProject(Long projectId) {
        List<Resource> assigned = resourceRepository.findByAssignedProjectId(projectId);
        assigned.forEach(r -> {
            r.setAvailable(true);
            r.setAssignedProjectId(null);
            r.setAssignedProjectName(null);
            resourceRepository.save(r);
        });
        return assigned.size();
    }

    @Transactional
    public void deleteResource(Long id) {
        findOrThrow(id);
        resourceRepository.deleteById(id);
    }

    private Resource findOrThrow(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recurso no encontrado con id: " + id));
    }

    private ResourceResponse toResponse(Resource r) {
        return ResourceResponse.builder()
                .id(r.getId()).name(r.getName()).email(r.getEmail())
                .department(r.getDepartment()).role(r.getRole().name())
                .available(r.isAvailable()).skills(r.getSkills())
                .assignedProjectId(r.getAssignedProjectId())
                .assignedProjectName(r.getAssignedProjectName())
                .createdAt(r.getCreatedAt()).build();
    }
}