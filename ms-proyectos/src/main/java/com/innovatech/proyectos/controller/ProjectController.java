package com.innovatech.proyectos.controller;

import com.innovatech.proyectos.config.JwtExtractor;
import com.innovatech.proyectos.dto.ProjectDtos.*;
import com.innovatech.proyectos.model.Project;
import com.innovatech.proyectos.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST del microservicio ms-proyectos.
 * Control de acceso basado en roles extraídos del JWT:
 *   - ADMIN: CRUD completo + asignar empleado + revisar notas
 *   - MANAGER: asignar empleado + revisar notas (solo lectura en proyectos)
 *   - EMPLOYEE: ver proyectos asignados + agregar notas de avance
 *
 * @author Benjamin Valdes, Ignacio Munoz
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProjectController {

    private final ProjectService projectService;
    private final JwtExtractor jwtExtractor;

    // ── Creación ─────────────────────────────────────────────────────────────

    /**
     * POST /api/projects
     * Solo ADMIN puede crear proyectos.
     */
    @PostMapping
    public ResponseEntity<?> createProject(
            @Valid @RequestBody CreateProjectRequest request,
            @RequestHeader("Authorization") String authHeader) {

        if (!jwtExtractor.isAdmin(authHeader)) {
            return forbidden("Solo los administradores pueden crear proyectos");
        }
        ProjectResponse response = projectService.createProject(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── Consultas ────────────────────────────────────────────────────────────

    /**
     * GET /api/projects
     * - ADMIN/MANAGER: todos los proyectos
     * - EMPLOYEE: solo sus proyectos asignados
     */
    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAllProjects(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestHeader("Authorization") String authHeader) {

        if (jwtExtractor.isEmployee(authHeader)) {
            Long userId = jwtExtractor.extractUserId(authHeader);
            return ResponseEntity.ok(projectService.getProjectsByAssignedUser(userId));
        }

        if (status != null) {
            return ResponseEntity.ok(projectService.getProjectsByStatus(
                    Project.ProjectStatus.valueOf(status.toUpperCase())));
        }
        if (type != null) {
            return ResponseEntity.ok(projectService.getProjectsByType(
                    Project.ProjectType.valueOf(type.toUpperCase())));
        }
        return ResponseEntity.ok(projectService.getAllProjects());
    }

    /**
     * GET /api/projects/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.getProjectById(id));
    }

    /**
     * GET /api/projects/manager/{managerId}
     */
    @GetMapping("/manager/{managerId}")
    public ResponseEntity<List<ProjectResponse>> getByManager(@PathVariable Long managerId) {
        return ResponseEntity.ok(projectService.getProjectsByManager(managerId));
    }

    // ── Edición (Admin) ──────────────────────────────────────────────────────

    /**
     * PUT /api/projects/{id}
     * ADMIN: edita nombre, descripción y/o estado de un proyecto.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProject(
            @PathVariable Long id,
            @RequestBody UpdateProjectRequest request,
            @RequestHeader("Authorization") String authHeader) {

        if (!jwtExtractor.isAdmin(authHeader)) {
            return forbidden("Solo los administradores pueden editar proyectos");
        }
        return ResponseEntity.ok(projectService.updateProject(id, request));
    }

    /**
     * PATCH /api/projects/{id}/status
     * ADMIN: cambia el estado de un proyecto.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request,
            @RequestHeader("Authorization") String authHeader) {

        if (!jwtExtractor.isAdmin(authHeader)) {
            return forbidden("Solo los administradores pueden cambiar el estado");
        }
        return ResponseEntity.ok(projectService.updateStatus(id, request));
    }

    // ── Asignación de empleado (Admin + Manager) ──────────────────────────────

    /**
     * PATCH /api/projects/{id}/assign
     * ADMIN o MANAGER: asigna un empleado real al proyecto.
     * El empleado debe existir en ms-auth con rol EMPLOYEE.
     */
    @PatchMapping("/{id}/assign")
    public ResponseEntity<?> assignEmployee(
            @PathVariable Long id,
            @Valid @RequestBody AssignEmployeeRequest request,
            @RequestHeader("Authorization") String authHeader) {

        if (!jwtExtractor.isAdminOrManager(authHeader)) {
            return forbidden("Solo administradores y managers pueden asignar empleados");
        }
        return ResponseEntity.ok(projectService.assignEmployee(id, request));
    }

    /**
     * DELETE /api/projects/{id}/assign
     * ADMIN o MANAGER: desasigna el empleado del proyecto.
     */
    @DeleteMapping("/{id}/assign")
    public ResponseEntity<?> unassignEmployee(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {

        if (!jwtExtractor.isAdminOrManager(authHeader)) {
            return forbidden("Solo administradores y managers pueden desasignar empleados");
        }
        return ResponseEntity.ok(projectService.unassignEmployee(id));
    }

    // ── Eliminación (Admin) ───────────────────────────────────────────────────

    /**
     * DELETE /api/projects/{id}
     * ADMIN: elimina un proyecto.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProject(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {

        if (!jwtExtractor.isAdmin(authHeader)) {
            return forbidden("Solo los administradores pueden eliminar proyectos");
        }
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    // ── Notas de avance ───────────────────────────────────────────────────────

    /**
     * POST /api/projects/{id}/notes
     * EMPLOYEE: agrega una nota de avance al proyecto (debe estar asignado).
     * La nota queda en estado PENDING hasta que Admin/Manager la revise.
     */
    @PostMapping("/{id}/notes")
    public ResponseEntity<?> addNote(
            @PathVariable Long id,
            @Valid @RequestBody CreateNoteRequest request,
            @RequestHeader("Authorization") String authHeader) {

        // Cualquier rol puede agregar notas, pero los empleados son los principales
        Long authorId = jwtExtractor.extractUserId(authHeader);
        String authorName = jwtExtractor.extractSubject(authHeader);

        if (authorId == null) {
            return forbidden("Token inválido");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.addNote(id, request, authorId, authorName));
    }

    /**
     * GET /api/projects/{id}/notes
     * Todos los roles: obtiene las notas de un proyecto.
     */
    @GetMapping("/{id}/notes")
    public ResponseEntity<List<NoteResponse>> getNotes(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.getNotesByProject(id));
    }

    /**
     * PATCH /api/projects/{id}/notes/{noteId}/review
     * ADMIN o MANAGER: aprueba o rechaza una nota de avance.
     * Flujo similar a aprobar/rechazar un Pull Request en GitHub.
     */
    @PatchMapping("/{id}/notes/{noteId}/review")
    public ResponseEntity<?> reviewNote(
            @PathVariable Long id,
            @PathVariable Long noteId,
            @Valid @RequestBody ReviewNoteRequest request,
            @RequestHeader("Authorization") String authHeader) {

        if (!jwtExtractor.isAdminOrManager(authHeader)) {
            return forbidden("Solo administradores y managers pueden revisar notas");
        }

        Long reviewerId = jwtExtractor.extractUserId(authHeader);
        String reviewerName = jwtExtractor.extractSubject(authHeader);

        return ResponseEntity.ok(
                projectService.reviewNote(id, noteId, request, reviewerId, reviewerName));
    }

    // ── Health ────────────────────────────────────────────────────────────────

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "ms-proyectos",
                "version", "1.0.0",
                "totalProjects", projectService.getTotalCount()
        ));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private ResponseEntity<Map<String, String>> forbidden(String message) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Forbidden", "message", message));
    }
}