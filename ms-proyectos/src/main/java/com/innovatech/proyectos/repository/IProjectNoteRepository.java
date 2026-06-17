package com.innovatech.proyectos.repository;

import com.innovatech.proyectos.model.ProjectNote;

import java.util.List;
import java.util.Optional;

/**
 * Interfaz de dominio para notas de proyecto.
 * Patrón Repository: abstrae la persistencia de las notas.
 */
public interface IProjectNoteRepository {
    List<ProjectNote> findByProjectId(Long projectId);
    List<ProjectNote> findByProjectIdAndStatus(Long projectId, ProjectNote.NoteStatus status);
    List<ProjectNote> findByAuthorId(Long authorId);
    Optional<ProjectNote> findById(Long id);
    ProjectNote save(ProjectNote note);
    void deleteById(Long id);
    boolean existsById(Long id);
}