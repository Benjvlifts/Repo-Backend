package com.innovatech.recursos.controller;

import com.innovatech.recursos.dto.ResourceDtos.*;
import com.innovatech.recursos.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Recursos", description = "Gestión de recursos humanos: disponibilidad, asignación y consulta por departamento")
public class ResourceController {

    private final ResourceService resourceService;

    @PostMapping
    @Operation(summary = "Crear recurso", description = "Registra un nuevo recurso humano en el sistema.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Recurso creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "409", description = "Ya existe un recurso con ese email")
    })
    public ResponseEntity<ResourceResponse> create(@Valid @RequestBody CreateResourceRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resourceService.createResource(req));
    }

    @GetMapping
    @Operation(summary = "Listar recursos", description = "Retorna todos los recursos registrados.")
    @ApiResponse(responseCode = "200", description = "Lista de recursos")
    public ResponseEntity<List<ResourceResponse>> getAll() {
        return ResponseEntity.ok(resourceService.getAllResources());
    }

    @GetMapping("/available")
    @Operation(summary = "Listar recursos disponibles", description = "Retorna únicamente los recursos marcados como disponibles.")
    @ApiResponse(responseCode = "200", description = "Lista de recursos disponibles")
    public ResponseEntity<List<ResourceResponse>> getAvailable() {
        return ResponseEntity.ok(resourceService.getAvailableResources());
    }

    @GetMapping("/department/{dept}")
    @Operation(summary = "Listar recursos por departamento", description = "Filtra los recursos según el departamento indicado.")
    @ApiResponse(responseCode = "200", description = "Lista de recursos filtrados")
    public ResponseEntity<List<ResourceResponse>> getByDepartment(@PathVariable String dept) {
        return ResponseEntity.ok(resourceService.getByDepartment(dept));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener recurso por ID", description = "Retorna el detalle de un recurso específico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recurso encontrado"),
            @ApiResponse(responseCode = "404", description = "Recurso no encontrado")
    })
    public ResponseEntity<ResourceResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(resourceService.getById(id));
    }

    @PatchMapping("/{id}/availability")
    @Operation(summary = "Actualizar disponibilidad", description = "Cambia el estado de disponibilidad de un recurso.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Disponibilidad actualizada"),
            @ApiResponse(responseCode = "404", description = "Recurso no encontrado")
    })
    public ResponseEntity<ResourceResponse> updateAvailability(
            @PathVariable Long id,
            @RequestBody UpdateAvailabilityRequest req) {
        return ResponseEntity.ok(resourceService.updateAvailability(id, req));
    }

    @PatchMapping("/{id}/assign")
    @Operation(summary = "Asignar recurso a un proyecto", description = "Asocia un recurso a un proyecto específico. Marca el recurso como no disponible.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recurso asignado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Recurso no encontrado")
    })
    public ResponseEntity<ResourceResponse> assignToProject(
            @PathVariable Long id,
            @Valid @RequestBody AssignToProjectRequest req) {
        return ResponseEntity.ok(resourceService.assignToProject(id, req.getProjectId(), req.getProjectName()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar recurso", description = "Elimina un recurso de forma permanente.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Recurso eliminado"),
            @ApiResponse(responseCode = "404", description = "Recurso no encontrado")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        resourceService.deleteResource(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/health")
    @Operation(summary = "Estado del servicio", description = "Endpoint de health check de ms-recursos.")
    @ApiResponse(responseCode = "200", description = "Servicio activo")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "ms-recursos", "version", "1.0.0"));
    }

        @PutMapping("/capacity")
    @Operation(summary = "Liberar recursos de un proyecto",
               description = "Marca como disponibles todos los recursos asignados al proyecto indicado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recursos liberados correctamente"),
            @ApiResponse(responseCode = "400", description = "projectId es requerido")
    })
    public ResponseEntity<Map<String, Object>> releaseCapacity(@RequestParam Long projectId) {
        int released = resourceService.releaseByProject(projectId);
        return ResponseEntity.ok(Map.of(
            "projectId", projectId,
            "resourcesReleased", released,
            "message", released + " recurso(s) liberado(s) del proyecto " + projectId
        ));
    }
}