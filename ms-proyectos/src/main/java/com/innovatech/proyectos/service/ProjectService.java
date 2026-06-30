package com.innovatech.proyectos.service;

import com.innovatech.proyectos.dto.ProjectDtos.*;
import com.innovatech.proyectos.messaging.ProjectEventProducer;
import com.innovatech.proyectos.model.Project;
import com.innovatech.proyectos.model.ProjectNote;
import com.innovatech.proyectos.patterns.factory.ProjectFactory;
import com.innovatech.proyectos.patterns.factory.ProjectFactoryProvider;
import com.innovatech.proyectos.repository.IProjectNoteRepository;
import com.innovatech.proyectos.repository.IProjectRepository;
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

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request) {
        ProjectFactory factory = factoryProvider.getFactory(request.getType());
        Project project = factory.createProject(request);
        Project saved = projectRepository.save(project);
        eventProducer.publishProjectCreated(saved.getId(), saved.getName(),
                saved.getType().name(), saved.getStatus().name(), saved.getManagerId());
        return toResponse(saved);
    }

    @Transactional
    public ProjectResponse updateProject(Long id, UpdateProjectRequest request) {
        Project project = findOrThrow(id);
        if (request.getName() != null && !request.getName().isBlank())
            project.setName(request.getName());
        if (request.getDescription() != null)
            project.setDescription(request.getDescription());
        if (request.getStatus() != null)
            project.setStatus(request.getStatus());
        return toResponse(projectRepository.save(project));
    }

    /**
     * FIX: publica evento de cambio de estado para que ms-recursos
     * libere automáticamente los recursos cuando el proyecto finaliza.
     */
    @Transactional
    public ProjectResponse updateStatus(Long id, UpdateStatusRequest request) {
        Project project = findOrThrow(id);
        project.setStatus(request.getStatus());
        Project saved = projectRepository.save(project);
        // FIX: notifica a consumidores; ms-recursos libera recursos si status=COMPLETED
        eventProducer.publishProjectEvent(saved.getId(), saved.getName(),
                saved.getType().name(), saved.getStatus().name(), saved.getManagerId());
        return toResponse(saved);
    }

    @Transactional
    public ProjectResponse assignEmployee(Long projectId, AssignEmployeeRequest request) {
        Project project = findOrThrow(projectId);
        project.setAssignedUserId(request.getEmployeeId());
        project.setAssignedUserName(request.getEmployeeName());
        Project saved = projectRepository.save(project);
        // NUEVO: dispara evento para que ms-recursos marque el recurso como no disponible
        eventProducer.publishResourceAssigned(
                saved.getId(), saved.getName(),
                request.getEmployeeId(), request.getEmployeeName());
        return toResponse(saved);
    }

    @Transactional
    public ProjectResponse unassignEmployee(Long projectId) {
        Project project = findOrThrow(projectId);
        Long   prevEmployeeId   = project.getAssignedUserId();
        String prevEmployeeName = project.getAssignedUserName();
        project.setAssignedUserId(null);
        project.setAssignedUserName(null);
        Project saved = projectRepository.save(project);
        // NUEVO: dispara evento para que ms-recursos libere el recurso
        if (prevEmployeeId != null) {
            eventProducer.publishResourceUnassigned(
                    saved.getId(), saved.getName(),
                    prevEmployeeId, prevEmployeeName);
        }
        return toResponse(saved);
    }

    @Transactional
    public void deleteProject(Long id) {
        if (!projectRepository.existsById(id))
            throw new IllegalArgumentException("Proyecto no encontrado con id: " + id);
        projectRepository.deleteById(id);
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    public List<ProjectResponse> getAllProjects() {
        return projectRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public ProjectResponse getProjectById(Long id) { return toResponse(findOrThrow(id)); }

    public List<ProjectResponse> getProjectsByStatus(Project.ProjectStatus status) {
        return projectRepository.findByStatus(status).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<ProjectResponse> getProjectsByType(Project.ProjectType type) {
        return projectRepository.findByType(type).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<ProjectResponse> getProjectsByManager(Long managerId) {
        return projectRepository.findByManagerId(managerId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<ProjectResponse> getProjectsByAssignedUser(Long userId) {
        return projectRepository.findByAssignedUserId(userId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public long getTotalCount() { return projectRepository.count(); }

    // ── Notas ─────────────────────────────────────────────────────────────────

    @Transactional
    public NoteResponse addNote(Long projectId, CreateNoteRequest request,
                                Long authorId, String authorName) {
        findOrThrow(projectId);
        ProjectNote note = ProjectNote.builder()
                .projectId(projectId).authorId(authorId).authorName(authorName)
                .content(request.getContent()).status(ProjectNote.NoteStatus.PENDING).build();
        return toNoteResponse(noteRepository.save(note));
    }

    public List<NoteResponse> getNotesByProject(Long projectId) {
        findOrThrow(projectId);
        return noteRepository.findByProjectId(projectId).stream()
                .map(this::toNoteResponse).collect(Collectors.toList());
    }

    /**
     * FIX Bug 8: valida que un MANAGER solo revise notas de proyectos que gestiona.
     * @param reviewerRole rol extraído del JWT ("ADMIN" | "MANAGER")
     */
    @Transactional
    public NoteResponse reviewNote(Long projectId, Long noteId,
                                   ReviewNoteRequest request,
                                   Long reviewerId, String reviewerName,
                                   String reviewerRole) {
        Project project = findOrThrow(projectId);

        // FIX Bug 8: MANAGER solo puede revisar sus propios proyectos
        if ("MANAGER".equalsIgnoreCase(reviewerRole)) {
            if (project.getManagerId() == null || !project.getManagerId().equals(reviewerId)) {
                throw new IllegalArgumentException(
                    "El manager (id=" + reviewerId + ") no gestiona el proyecto " + projectId);
            }
        }

        ProjectNote note = noteRepository.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException("Nota no encontrada con id: " + noteId));

        if (!note.getProjectId().equals(projectId))
            throw new IllegalArgumentException("La nota no pertenece al proyecto indicado");

        if (note.getStatus() != ProjectNote.NoteStatus.PENDING)
            throw new IllegalArgumentException("Solo se pueden revisar notas en estado PENDING");

        note.setStatus(request.getStatus());
        note.setReviewerId(reviewerId);
        note.setReviewerName(reviewerName);
        note.setReviewComment(request.getReviewComment());
        note.setReviewedAt(LocalDateTime.now());

        return toNoteResponse(noteRepository.save(note));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Project findOrThrow(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado con id: " + id));
    }

    private ProjectResponse toResponse(Project p) {
        return ProjectResponse.builder()
                .id(p.getId()).name(p.getName()).description(p.getDescription())
                .type(p.getType().name()).status(p.getStatus().name())
                .managerId(p.getManagerId()).assignedUserId(p.getAssignedUserId())
                .assignedUserName(p.getAssignedUserName()).startDate(p.getStartDate())
                .endDate(p.getEndDate()).techStack(p.getTechStack())
                .repositoryUrl(p.getRepositoryUrl()).clientName(p.getClientName())
                .slaDays(p.getSlaDays()).cloudProvider(p.getCloudProvider())
                .budgetUsd(p.getBudgetUsd()).createdAt(p.getCreatedAt()).build();
    }

    private NoteResponse toNoteResponse(ProjectNote n) {
        return NoteResponse.builder()
                .id(n.getId()).projectId(n.getProjectId()).authorId(n.getAuthorId())
                .authorName(n.getAuthorName()).content(n.getContent())
                .status(n.getStatus().name()).reviewerId(n.getReviewerId())
                .reviewerName(n.getReviewerName()).reviewComment(n.getReviewComment())
                .createdAt(n.getCreatedAt()).reviewedAt(n.getReviewedAt()).build();
    }
}