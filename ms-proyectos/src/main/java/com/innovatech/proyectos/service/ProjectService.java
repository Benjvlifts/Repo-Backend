package com.innovatech.proyectos.service;

import com.innovatech.proyectos.dto.ProjectDtos.*;
import com.innovatech.proyectos.messaging.ProjectEventProducer;
import com.innovatech.proyectos.model.Project;
import com.innovatech.proyectos.model.ProjectNote;
import com.innovatech.proyectos.patterns.factory.ProjectFactory;
import com.innovatech.proyectos.patterns.factory.ProjectFactoryProvider;
import com.innovatech.proyectos.repository.IProjectRepository;
import com.innovatech.proyectos.repository.IProjectNoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final IProjectRepository projectRepository;
    private final IProjectNoteRepository noteRepository;
    private final ProjectFactoryProvider factoryProvider;
    private final ProjectEventProducer eventProducer;

    // ── Creación (Factory Method + Kafka Event) ───────────────────────────────
    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request) {
        ProjectFactory factory = factoryProvider.getFactory(request.getType());
        Project project = factory.createProject(request);
        Project saved = projectRepository.save(project);

        eventProducer.publishProjectCreated(
            saved.getId(), saved.getName(),
            saved.getType().name(), saved.getStatus().name(),
            saved.getManagerId()
        );

        return toResponse(saved);
    }

    // ── Edición completa (Admin) ──────────────────────────────────────────────
    @Transactional
    public ProjectResponse updateProject(Long id, UpdateProjectRequest request) {
        Project project = findOrThrow(id);

        if (request.getName() != null && !request.getName().isBlank()) {
            project.setName(request.getName());
        }
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            project.setStatus(request.getStatus());
        }

        return toResponse(projectRepository.save(project));
    }

    // ── Actualización de estado (Admin) ──────────────────────────────────────
    @Transactional
    public ProjectResponse updateStatus(Long id, UpdateStatusRequest request) {
        Project project = findOrThrow(id);
        project.setStatus(request.getStatus());
        return toResponse(projectRepository.save(project));
    }

    // ── Asignar empleado (Admin + Manager) ────────────────────────────────────
    @Transactional
    public ProjectResponse assignEmployee(Long projectId, AssignEmployeeRequest request) {
        Project project = findOrThrow(projectId);
        project.setAssignedUserId(request.getEmployeeId());
        project.setAssignedUserName(request.getEmployeeName());
        return toResponse(projectRepository.save(project));
    }

    // ── Desasignar empleado (Admin + Manager) ────────────────────────────────
    @Transactional
    public ProjectResponse unassignEmployee(Long projectId) {
        Project project = findOrThrow(projectId);
        project.setAssignedUserId(null);
        project.setAssignedUserName(null);
        return toResponse(projectRepository.save(project));
    }

    // ── Eliminar (Admin) ──────────────────────────────────────────────────────
    @Transactional
    public void deleteProject(Long id) {
        if (!projectRepository.existsById(id))
            throw new IllegalArgumentException("Proyecto no encontrado con id: " + id);
        projectRepository.deleteById(id);
    }

    // ── Consultas ─────────────────────────────────────────────────────────────
    public List<ProjectResponse> getAllProjects() {
        return projectRepository.findAll().stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    public ProjectResponse getProjectById(Long id) {
        return toResponse(findOrThrow(id));
    }

    public List<ProjectResponse> getProjectsByStatus(Project.ProjectStatus status) {
        return projectRepository.findByStatus(status).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    public List<ProjectResponse> getProjectsByType(Project.ProjectType type) {
        return projectRepository.findByType(type).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    public List<ProjectResponse> getProjectsByManager(Long managerId) {
        return projectRepository.findByManagerId(managerId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Obtiene los proyectos donde el empleado está asignado.
     * Útil para la vista del empleado.
     */
    public List<ProjectResponse> getProjectsByAssignedUser(Long userId) {
        return projectRepository.findAll().stream()
                .filter(p -> userId.equals(p.getAssignedUserId()))
                .map(this::toResponse).collect(Collectors.toList());
    }

    public long getTotalCount() { return projectRepository.count(); }

    // ── Notas de avance ───────────────────────────────────────────────────────

    /**
     * Agrega una nota de avance a un proyecto.
     * Solo empleados asignados al proyecto pueden agregar notas.
     */
    @Transactional
    public NoteResponse addNote(Long projectId, CreateNoteRequest request,
                                Long authorId, String authorName) {
        // Verificar que el proyecto existe
        findOrThrow(projectId);

        ProjectNote note = ProjectNote.builder()
                .projectId(projectId)
                .authorId(authorId)
                .authorName(authorName)
                .content(request.getContent())
                .status(ProjectNote.NoteStatus.PENDING)
                .build();

        return toNoteResponse(noteRepository.save(note));
    }

    /**
     * Obtiene todas las notas de un proyecto.
     */
    public List<NoteResponse> getNotesByProject(Long projectId) {
        findOrThrow(projectId);
        return noteRepository.findByProjectId(projectId).stream()
                .map(this::toNoteResponse).collect(Collectors.toList());
    }

    /**
     * Revisa una nota: Admin o Manager puede aprobar o rechazar.
     * Similar al flujo de review de una Pull Request en GitHub.
     */
    @Transactional
    public NoteResponse reviewNote(Long projectId, Long noteId,
                                   ReviewNoteRequest request,
                                   Long reviewerId, String reviewerName) {
        // Verificar que el proyecto y la nota existen
        findOrThrow(projectId);

        ProjectNote note = noteRepository.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException("Nota no encontrada con id: " + noteId));

        if (!note.getProjectId().equals(projectId)) {
            throw new IllegalArgumentException("La nota no pertenece al proyecto indicado");
        }

        if (note.getStatus() != ProjectNote.NoteStatus.PENDING) {
            throw new IllegalArgumentException("Solo se pueden revisar notas en estado PENDING");
        }

        note.setStatus(request.getStatus());
        note.setReviewerId(reviewerId);
        note.setReviewerName(reviewerName);
        note.setReviewComment(request.getReviewComment());
        note.setReviewedAt(LocalDateTime.now());

        return toNoteResponse(noteRepository.save(note));
    }

    // ── Mappers ──────────────────────────────────────────────────────────────

    private Project findOrThrow(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado con id: " + id));
    }

    ProjectResponse toResponse(Project p) {
        return ProjectResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .type(p.getType().name())
                .status(p.getStatus().name())
                .managerId(p.getManagerId())
                .assignedUserId(p.getAssignedUserId())
                .assignedUserName(p.getAssignedUserName())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .techStack(p.getTechStack())
                .repositoryUrl(p.getRepositoryUrl())
                .clientName(p.getClientName())
                .slaDays(p.getSlaDays())
                .cloudProvider(p.getCloudProvider())
                .budgetUsd(p.getBudgetUsd())
                .createdAt(p.getCreatedAt())
                .build();
    }

    NoteResponse toNoteResponse(ProjectNote n) {
        return NoteResponse.builder()
                .id(n.getId())
                .projectId(n.getProjectId())
                .authorId(n.getAuthorId())
                .authorName(n.getAuthorName())
                .content(n.getContent())
                .status(n.getStatus().name())
                .reviewerId(n.getReviewerId())
                .reviewerName(n.getReviewerName())
                .reviewComment(n.getReviewComment())
                .createdAt(n.getCreatedAt())
                .reviewedAt(n.getReviewedAt())
                .build();
    }
}