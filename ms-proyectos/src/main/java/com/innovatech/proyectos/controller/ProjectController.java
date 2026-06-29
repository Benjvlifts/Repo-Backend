package com.innovatech.proyectos.controller;

import com.innovatech.proyectos.config.JwtExtractor;
import com.innovatech.proyectos.dto.ProjectDtos.*;
import com.innovatech.proyectos.model.Project;
import com.innovatech.proyectos.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@Tag(name = "Proyectos", description = "Gestión de proyectos, asignación de empleados y notas de avance")
public class ProjectController {

    private final ProjectService projectService;
    private final JwtExtractor jwtExtractor;

    // ── Creación ─────────────────────────────────────────────────────────────

    /**
     * POST /api/projects
     * Solo ADMIN puede crear proyectos.
     */
    @PostMapping
    @Operation(summary = "Crear proyecto", description = "Crea un nuevo proyecto. Solo accesible para usuarios con rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Proyecto creado correctamente"),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene rol ADMIN")
    })
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
    @Operation(summary = "Listar proyectos", description = "ADMIN/MANAGER ven todos los proyectos (con filtros opcionales por estado o tipo). EMPLOYEE solo ve sus proyectos asignados.")
    public ResponseEntity<List<ProjectResponse>> getAllProjects(
            @Parameter(description = "Filtra por estado del proyecto (ej. IN_PROGRESS, COMPLETED)")
            @RequestParam(required = false) String status,
            @Parameter(description = "Filtra por tipo de proyecto (ej. SOFTWARE, CONSULTING, INFRASTRUCTURE)")
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
    @Operation(summary = "Obtener proyecto por ID", description = "Retorna el detalle de un proyecto específico. Solo ADMIN o el manager asignado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proyecto encontrado"),
            @ApiResponse(responseCode = "403", description = "El usuario no es ADMIN ni el manager asignado"),
            @ApiResponse(responseCode = "404", description = "Proyecto no encontrado")
    })
    public ResponseEntity<?> getProjectById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {

        ProjectResponse project = projectService.getProjectById(id);

        Long requesterId = jwtExtractor.extractUserId(authHeader);
        boolean isAssignedManager = project.getManagerId() != null
                && project.getManagerId().equals(requesterId);

        if (!jwtExtractor.isAdmin(authHeader) && !isAssignedManager) {
            return forbidden("Solo ADMIN o el manager asignado pueden ver este proyecto");
        }
        return ResponseEntity.ok(project);
    }

    /**
     * GET /api/projects/manager/{managerId}
     */
    @GetMapping("/manager/{managerId}")
    @Operation(summary = "Listar proyectos por manager", description = "Retorna los proyectos asociados a un manager específico. Solo ADMIN o el propio manager.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proyectos del manager"),
            @ApiResponse(responseCode = "403", description = "El usuario no es ADMIN ni el manager consultado")
    })
    public ResponseEntity<?> getByManager(
            @PathVariable Long managerId,
            @RequestHeader("Authorization") String authHeader) {

        Long requesterId = jwtExtractor.extractUserId(authHeader);
        boolean isOwnManagerId = managerId.equals(requesterId);

        if (!jwtExtractor.isAdmin(authHeader) && !isOwnManagerId) {
            return forbidden("Solo ADMIN o el propio manager pueden ver estos proyectos");
        }
        return ResponseEntity.ok(projectService.getProjectsByManager(managerId));
    }

    // ── Edición (Admin) ──────────────────────────────────────────────────────

    /**
     * PUT /api/projects/{id}
     * ADMIN: edita nombre, descripción y/o estado de un proyecto.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar proyecto", description = "Edita nombre, descripción y/o estado de un proyecto. Solo ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proyecto actualizado"),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene rol ADMIN")
    })
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
    @Operation(summary = "Cambiar estado del proyecto", description = "Actualiza únicamente el estado de un proyecto. Solo ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado"),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene rol ADMIN")
    })
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
    @Operation(summary = "Asignar empleado a un proyecto", description = "Asigna un empleado (existente en ms-auth con rol EMPLOYEE) a un proyecto. ADMIN o MANAGER.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Empleado asignado correctamente"),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene rol ADMIN o MANAGER")
    })
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
    @Operation(summary = "Desasignar empleado de un proyecto", description = "Quita la asignación de empleado de un proyecto. ADMIN o MANAGER.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Empleado desasignado correctamente"),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene rol ADMIN o MANAGER")
    })
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
    @Operation(summary = "Eliminar proyecto", description = "Elimina un proyecto de forma permanente. Solo ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Proyecto eliminado"),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene rol ADMIN")
    })
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
    @Operation(summary = "Agregar nota de avance", description = "Agrega una nota de avance a un proyecto. Queda en estado PENDING hasta su revisión.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Nota creada correctamente"),
            @ApiResponse(responseCode = "403", description = "Token inválido")
    })
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
    @Operation(summary = "Listar notas de un proyecto", description = "Retorna todas las notas de avance asociadas a un proyecto.")
    public ResponseEntity<List<NoteResponse>> getNotes(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.getNotesByProject(id));
    }

    /**
     * PATCH /api/projects/{id}/notes/{noteId}/review
     * ADMIN o MANAGER: aprueba o rechaza una nota de avance.
     * Flujo similar a aprobar/rechazar un Pull Request en GitHub.
     */
        @PatchMapping("/{id}/notes/{noteId}/review")
    @Operation(summary = "Revisar nota de avance",
               description = "Aprueba o rechaza una nota. ADMIN revisa cualquier proyecto; MANAGER solo los que gestiona.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nota revisada correctamente"),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene rol ADMIN o MANAGER"),
            @ApiResponse(responseCode = "400", description = "Manager sin acceso a este proyecto")
    })
    public ResponseEntity<?> reviewNote(
            @PathVariable Long id,
            @PathVariable Long noteId,
            @Valid @RequestBody ReviewNoteRequest request,
            @RequestHeader("Authorization") String authHeader) {

        if (!jwtExtractor.isAdminOrManager(authHeader)) {
            return forbidden("Solo administradores y managers pueden revisar notas");
        }

        Long reviewerId   = jwtExtractor.extractUserId(authHeader);
        String reviewerName = jwtExtractor.extractSubject(authHeader);
        String reviewerRole = jwtExtractor.extractRole(authHeader); // FIX Bug 8

        return ResponseEntity.ok(
                projectService.reviewNote(id, noteId, request, reviewerId, reviewerName, reviewerRole));
    }

    // ── Health ────────────────────────────────────────────────────────────────

    @GetMapping("/health")
    @Operation(summary = "Estado del servicio", description = "Endpoint de health check de ms-proyectos.")
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
