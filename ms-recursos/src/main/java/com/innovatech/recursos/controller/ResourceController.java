package com.innovatech.recursos.controller;

import com.innovatech.recursos.dto.ResourceDtos.*;
import com.innovatech.recursos.service.ResourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST del microservicio ms-recursos.
 * Expone endpoints para gestión de recursos humanos.
 *
 * @author Benjamin Valdes, Ignacio Munoz
 */
@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ResourceController {

    private final ResourceService resourceService;

    @PostMapping
    public ResponseEntity<ResourceResponse> create(@Valid @RequestBody CreateResourceRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resourceService.createResource(req));
    }

    @GetMapping
    public ResponseEntity<List<ResourceResponse>> getAll() {
        return ResponseEntity.ok(resourceService.getAllResources());
    }

    @GetMapping("/available")
    public ResponseEntity<List<ResourceResponse>> getAvailable() {
        return ResponseEntity.ok(resourceService.getAvailableResources());
    }

    @GetMapping("/department/{dept}")
    public ResponseEntity<List<ResourceResponse>> getByDepartment(@PathVariable String dept) {
        return ResponseEntity.ok(resourceService.getByDepartment(dept));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResourceResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(resourceService.getById(id));
    }

    @PatchMapping("/{id}/availability")
    public ResponseEntity<ResourceResponse> updateAvailability(
            @PathVariable Long id,
            @RequestBody UpdateAvailabilityRequest req) {
        return ResponseEntity.ok(resourceService.updateAvailability(id, req));
    }

    @PatchMapping("/{id}/assign")
    public ResponseEntity<ResourceResponse> assignToProject(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Long projectId = Long.valueOf(body.get("projectId").toString());
        String projectName = body.get("projectName").toString();
        return ResponseEntity.ok(resourceService.assignToProject(id, projectId, projectName));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        resourceService.deleteResource(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "ms-recursos", "version", "1.0.0"));
    }
}