package com.innovatech.proyectos.repository;

import com.innovatech.proyectos.model.ProjectNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Implementación JPA del repositorio de notas de proyecto.
 * Hereda de JpaRepository (Spring Data) e IProjectNoteRepository (dominio).
 */
@Repository
public interface JpaProjectNoteRepository extends JpaRepository<ProjectNote, Long>, IProjectNoteRepository {
    @Override
    List<ProjectNote> findByProjectId(Long projectId);

    @Override
    List<ProjectNote> findByProjectIdAndStatus(Long projectId, ProjectNote.NoteStatus status);

    @Override
    List<ProjectNote> findByAuthorId(Long authorId);
}