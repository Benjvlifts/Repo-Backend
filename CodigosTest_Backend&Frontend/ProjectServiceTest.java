package com.innovatech.proyectos;

import com.innovatech.proyectos.dto.ProjectDtos.*;
import com.innovatech.proyectos.messaging.ProjectEventProducer;
import com.innovatech.proyectos.model.Project;
import com.innovatech.proyectos.model.ProjectNote;
import com.innovatech.proyectos.patterns.factory.ProjectFactory;
import com.innovatech.proyectos.patterns.factory.ProjectFactoryProvider;
import com.innovatech.proyectos.repository.IProjectNoteRepository;
import com.innovatech.proyectos.repository.IProjectRepository;
import com.innovatech.proyectos.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para ProjectService.
 * Cubre: CRUD de proyectos, asignación de empleados, notas de avance, Factory pattern.
 * Cobertura objetivo: ≥75% de ProjectService.
 *
 * @author Benjamin Valdes, Ignacio Munoz
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectService — Pruebas Unitarias")
class ProjectServiceTest {

    @Mock private IProjectRepository projectRepository;
    @Mock private IProjectNoteRepository noteRepository;
    @Mock private ProjectFactoryProvider factoryProvider;
    @Mock private ProjectEventProducer eventProducer;
    @Mock private ProjectFactory projectFactory;

    @InjectMocks
    private ProjectService projectService;

    private Project sampleProject;
    private ProjectNote sampleNote;

    @BeforeEach
    void setUp() {
        sampleProject = Project.builder()
                .id(1L)
                .name("Portal Cliente Innovatech")
                .description("Desarrollo del portal web para clientes")
                .type(Project.ProjectType.SOFTWARE)
                .status(Project.ProjectStatus.PLANNING)
                .managerId(10L)
                .techStack("React, Spring Boot, PostgreSQL")
                .createdAt(LocalDateTime.now())
                .build();

        sampleNote = ProjectNote.builder()
                .id(1L)
                .projectId(1L)
                .authorId(5L)
                .authorName("Empleado Test")
                .content("Completé el módulo de autenticación")
                .status(ProjectNote.NoteStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ═══════════════════════════════════════════════════════════════
    // CREACIÓN DE PROYECTOS
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createProject()")
    class CreateProjectTests {

        @Test
        @DisplayName("✅ Crea proyecto SOFTWARE y publica evento Kafka")
        void createProject_softwareType_savesAndPublishesEvent() {
            CreateProjectRequest request = CreateProjectRequest.builder()
                    .name("Portal Cliente")
                    .type(Project.ProjectType.SOFTWARE)
                    .managerId(10L)
                    .build();

            when(factoryProvider.getFactory(Project.ProjectType.SOFTWARE)).thenReturn(projectFactory);
            when(projectFactory.createProject(request)).thenReturn(sampleProject);
            when(projectRepository.save(sampleProject)).thenReturn(sampleProject);
            doNothing().when(eventProducer).publishProjectCreated(anyLong(), anyString(), anyString(), anyString(), anyLong());

            ProjectResponse response = projectService.createProject(request);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getName()).isEqualTo("Portal Cliente Innovatech");
            assertThat(response.getType()).isEqualTo("SOFTWARE");

            verify(projectRepository, times(1)).save(sampleProject);
            verify(eventProducer, times(1)).publishProjectCreated(
                    eq(1L), anyString(), eq("SOFTWARE"), eq("PLANNING"), eq(10L));
        }

        @Test
        @DisplayName("✅ Crea proyecto CONSULTING usando factory correspondiente")
        void createProject_consultingType_usesConsultingFactory() {
            Project consultingProject = Project.builder()
                    .id(2L).name("Consultoría ERP")
                    .type(Project.ProjectType.CONSULTING)
                    .status(Project.ProjectStatus.PLANNING)
                    .managerId(10L).createdAt(LocalDateTime.now()).build();

            CreateProjectRequest request = CreateProjectRequest.builder()
                    .name("Consultoría ERP")
                    .type(Project.ProjectType.CONSULTING)
                    .managerId(10L).build();

            when(factoryProvider.getFactory(Project.ProjectType.CONSULTING)).thenReturn(projectFactory);
            when(projectFactory.createProject(request)).thenReturn(consultingProject);
            when(projectRepository.save(consultingProject)).thenReturn(consultingProject);
            doNothing().when(eventProducer).publishProjectCreated(anyLong(), anyString(), anyString(), anyString(), anyLong());

            ProjectResponse response = projectService.createProject(request);

            assertThat(response.getType()).isEqualTo("CONSULTING");
            verify(factoryProvider).getFactory(Project.ProjectType.CONSULTING);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ACTUALIZACIÓN DE PROYECTOS
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateProject()")
    class UpdateProjectTests {

        @Test
        @DisplayName("✅ Actualiza nombre y descripción de proyecto existente")
        void updateProject_existingId_updatesAndReturnsResponse() {
            UpdateProjectRequest request = UpdateProjectRequest.builder()
                    .name("Portal Cliente v2")
                    .description("Nueva descripción actualizada")
                    .build();

            Project updatedProject = Project.builder()
                    .id(1L).name("Portal Cliente v2")
                    .description("Nueva descripción actualizada")
                    .type(Project.ProjectType.SOFTWARE)
                    .status(Project.ProjectStatus.PLANNING)
                    .managerId(10L).createdAt(LocalDateTime.now()).build();

            when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
            when(projectRepository.save(any(Project.class))).thenReturn(updatedProject);

            ProjectResponse response = projectService.updateProject(1L, request);

            assertThat(response.getName()).isEqualTo("Portal Cliente v2");
            assertThat(response.getDescription()).isEqualTo("Nueva descripción actualizada");
        }

        @Test
        @DisplayName("❌ Lanza IllegalArgumentException si proyecto no existe")
        void updateProject_nonExistingId_throwsIllegalArgumentException() {
            when(projectRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.updateProject(99L,
                    UpdateProjectRequest.builder().name("x").build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no encontrado");
        }

        @Test
        @DisplayName("✅ Actualiza solo el estado del proyecto")
        void updateStatus_existingProject_updatesStatusOnly() {
            UpdateStatusRequest request = new UpdateStatusRequest(Project.ProjectStatus.IN_PROGRESS);

            when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
            when(projectRepository.save(any())).thenAnswer(inv -> {
                Project p = inv.getArgument(0);
                assertThat(p.getStatus()).isEqualTo(Project.ProjectStatus.IN_PROGRESS);
                return p;
            });

            projectService.updateStatus(1L, request);

            verify(projectRepository).save(any(Project.class));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ASIGNACIÓN DE EMPLEADOS
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("assignEmployee() / unassignEmployee()")
    class AssignEmployeeTests {

        @Test
        @DisplayName("✅ Asigna empleado a proyecto existente")
        void assignEmployee_existingProject_setsEmployeeFields() {
            AssignEmployeeRequest request = AssignEmployeeRequest.builder()
                    .employeeId(5L)
                    .employeeName("Juan Pérez")
                    .build();

            when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
            when(projectRepository.save(any())).thenAnswer(inv -> {
                Project p = inv.getArgument(0);
                assertThat(p.getAssignedUserId()).isEqualTo(5L);
                assertThat(p.getAssignedUserName()).isEqualTo("Juan Pérez");
                return p;
            });

            projectService.assignEmployee(1L, request);

            verify(projectRepository).save(any(Project.class));
        }

        @Test
        @DisplayName("✅ Desasigna empleado de proyecto — campos quedan en null")
        void unassignEmployee_existingProject_clearsEmployeeFields() {
            sampleProject.setAssignedUserId(5L);
            sampleProject.setAssignedUserName("Juan Pérez");

            when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
            when(projectRepository.save(any())).thenAnswer(inv -> {
                Project p = inv.getArgument(0);
                assertThat(p.getAssignedUserId()).isNull();
                assertThat(p.getAssignedUserName()).isNull();
                return p;
            });

            projectService.unassignEmployee(1L);

            verify(projectRepository).save(any(Project.class));
        }

        @Test
        @DisplayName("❌ Asignar a proyecto inexistente lanza excepción")
        void assignEmployee_nonExistingProject_throwsException() {
            when(projectRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.assignEmployee(99L,
                    AssignEmployeeRequest.builder().employeeId(1L).build()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ELIMINACIÓN
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("deleteProject()")
    class DeleteProjectTests {

        @Test
        @DisplayName("✅ Elimina proyecto existente sin errores")
        void deleteProject_existingId_deletesSuccessfully() {
            when(projectRepository.existsById(1L)).thenReturn(true);
            doNothing().when(projectRepository).deleteById(1L);

            assertThatCode(() -> projectService.deleteProject(1L)).doesNotThrowAnyException();

            verify(projectRepository).deleteById(1L);
        }

        @Test
        @DisplayName("❌ Lanza excepción al intentar eliminar proyecto inexistente")
        void deleteProject_nonExistingId_throwsIllegalArgumentException() {
            when(projectRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> projectService.deleteProject(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no encontrado");

            verify(projectRepository, never()).deleteById(any());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // CONSULTAS
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Consultas de proyectos")
    class QueryTests {

        @Test
        @DisplayName("✅ getAllProjects retorna lista completa")
        void getAllProjects_returnsAllProjects() {
            when(projectRepository.findAll()).thenReturn(List.of(sampleProject));

            List<ProjectResponse> result = projectService.getAllProjects();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Portal Cliente Innovatech");
        }

        @Test
        @DisplayName("✅ getProjectById retorna proyecto correcto")
        void getProjectById_existingId_returnsProject() {
            when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));

            ProjectResponse response = projectService.getProjectById(1L);

            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getManagerId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("✅ getProjectsByStatus filtra por estado correctamente")
        void getProjectsByStatus_planning_returnsFilteredProjects() {
            when(projectRepository.findByStatus(Project.ProjectStatus.PLANNING))
                    .thenReturn(List.of(sampleProject));

            List<ProjectResponse> result = projectService.getProjectsByStatus(Project.ProjectStatus.PLANNING);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo("PLANNING");
        }

        @Test
        @DisplayName("✅ getProjectsByAssignedUser filtra en memoria por userId")
        void getProjectsByAssignedUser_returnsOnlyAssignedProjects() {
            sampleProject.setAssignedUserId(5L);
            Project otherProject = Project.builder()
                    .id(2L).name("Otro").type(Project.ProjectType.SOFTWARE)
                    .status(Project.ProjectStatus.PLANNING).assignedUserId(99L)
                    .createdAt(LocalDateTime.now()).build();

            when(projectRepository.findAll()).thenReturn(List.of(sampleProject, otherProject));

            List<ProjectResponse> result = projectService.getProjectsByAssignedUser(5L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("✅ getTotalCount retorna el conteo correcto del repositorio")
        void getTotalCount_returnsTotalFromRepository() {
            when(projectRepository.count()).thenReturn(7L);

            assertThat(projectService.getTotalCount()).isEqualTo(7L);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // NOTAS DE AVANCE
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Notas de avance")
    class NoteTests {

        @Test
        @DisplayName("✅ addNote crea nota PENDING para proyecto existente")
        void addNote_existingProject_createsNoteAsPending() {
            CreateNoteRequest request = new CreateNoteRequest("Completé el módulo de login");
            when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
            when(noteRepository.save(any(ProjectNote.class))).thenReturn(sampleNote);

            NoteResponse response = projectService.addNote(1L, request, 5L, "Empleado Test");

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo("PENDING");
            assertThat(response.getAuthorId()).isEqualTo(5L);
            verify(noteRepository).save(any(ProjectNote.class));
        }

        @Test
        @DisplayName("❌ addNote falla si proyecto no existe")
        void addNote_nonExistingProject_throwsException() {
            when(projectRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.addNote(99L,
                    new CreateNoteRequest("contenido"), 1L, "User"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("✅ getNotesByProject retorna notas del proyecto")
        void getNotesByProject_existingProject_returnsNotes() {
            when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
            when(noteRepository.findByProjectId(1L)).thenReturn(List.of(sampleNote));

            List<NoteResponse> notes = projectService.getNotesByProject(1L);

            assertThat(notes).hasSize(1);
            assertThat(notes.get(0).getContent()).isEqualTo("Completé el módulo de autenticación");
        }

        @Test
        @DisplayName("✅ reviewNote aprueba nota PENDING y guarda reviewer")
        void reviewNote_pendingNote_approvesAndSetsReviewer() {
            ReviewNoteRequest request = ReviewNoteRequest.builder()
                    .status(ProjectNote.NoteStatus.APPROVED)
                    .reviewComment("Excelente trabajo")
                    .build();

            ProjectNote reviewedNote = ProjectNote.builder()
                    .id(1L).projectId(1L).authorId(5L).content("contenido")
                    .status(ProjectNote.NoteStatus.APPROVED)
                    .reviewerId(10L).reviewerName("Manager Test")
                    .reviewComment("Excelente trabajo").createdAt(LocalDateTime.now()).build();

            when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
            when(noteRepository.findById(1L)).thenReturn(Optional.of(sampleNote));
            when(noteRepository.save(any(ProjectNote.class))).thenReturn(reviewedNote);

            NoteResponse response = projectService.reviewNote(1L, 1L, request, 10L, "Manager Test");

            assertThat(response.getStatus()).isEqualTo("APPROVED");
            assertThat(response.getReviewComment()).isEqualTo("Excelente trabajo");
        }

        @Test
        @DisplayName("❌ reviewNote falla si nota ya fue revisada (no PENDING)")
        void reviewNote_alreadyReviewedNote_throwsException() {
            sampleNote.setStatus(ProjectNote.NoteStatus.APPROVED);

            when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
            when(noteRepository.findById(1L)).thenReturn(Optional.of(sampleNote));

            assertThatThrownBy(() -> projectService.reviewNote(1L, 1L,
                    ReviewNoteRequest.builder().status(ProjectNote.NoteStatus.REJECTED).build(),
                    10L, "Manager"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("PENDING");
        }

        @Test
        @DisplayName("❌ reviewNote falla si nota no pertenece al proyecto")
        void reviewNote_noteFromDifferentProject_throwsException() {
            sampleNote.setProjectId(99L); // nota de otro proyecto

            when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
            when(noteRepository.findById(1L)).thenReturn(Optional.of(sampleNote));

            assertThatThrownBy(() -> projectService.reviewNote(1L, 1L,
                    ReviewNoteRequest.builder().status(ProjectNote.NoteStatus.APPROVED).build(),
                    10L, "Manager"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no pertenece");
        }
    }
}