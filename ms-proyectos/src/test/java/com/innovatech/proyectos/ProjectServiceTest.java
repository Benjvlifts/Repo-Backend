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
                .id(1L).name("Portal Cliente Innovatech").description("Descripción")
                .type(Project.ProjectType.SOFTWARE).status(Project.ProjectStatus.PLANNING)
                .managerId(10L).techStack("React, Spring Boot").createdAt(LocalDateTime.now())
                .build();

        sampleNote = ProjectNote.builder()
                .id(1L).projectId(1L).authorId(5L).authorName("Empleado Test")
                .content("Completé el módulo de autenticación")
                .status(ProjectNote.NoteStatus.PENDING).createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("createProject()")
    class CreateProjectTests {

        @Test
        @DisplayName("✅ Crea proyecto SOFTWARE y publica evento Kafka")
        void createProject_softwareType_savesAndPublishesEvent() {
            CreateProjectRequest request = CreateProjectRequest.builder()
                    .name("Portal Cliente").type(Project.ProjectType.SOFTWARE).managerId(10L).build();

            when(factoryProvider.getFactory(Project.ProjectType.SOFTWARE)).thenReturn(projectFactory);
            when(projectFactory.createProject(request)).thenReturn(sampleProject);
            when(projectRepository.save(sampleProject)).thenReturn(sampleProject);
            doNothing().when(eventProducer).publishProjectCreated(anyLong(), anyString(), anyString(), anyString(), anyLong());

            ProjectResponse response = projectService.createProject(request);

            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getType()).isEqualTo("SOFTWARE");
            verify(projectRepository).save(sampleProject);
            verify(eventProducer).publishProjectCreated(eq(1L), anyString(), eq("SOFTWARE"), eq("PLANNING"), eq(10L));
        }

        @Test
        @DisplayName("✅ Crea proyecto CONSULTING usando factory correspondiente")
        void createProject_consultingType_usesConsultingFactory() {
            Project consultingProject = Project.builder().id(2L).name("Consultoría ERP")
                    .type(Project.ProjectType.CONSULTING).status(Project.ProjectStatus.PLANNING)
                    .managerId(10L).createdAt(LocalDateTime.now()).build();

            CreateProjectRequest request = CreateProjectRequest.builder()
                    .name("Consultoría ERP").type(Project.ProjectType.CONSULTING).managerId(10L).build();

            when(factoryProvider.getFactory(Project.ProjectType.CONSULTING)).thenReturn(projectFactory);
            when(projectFactory.createProject(request)).thenReturn(consultingProject);
            when(projectRepository.save(consultingProject)).thenReturn(consultingProject);
            doNothing().when(eventProducer).publishProjectCreated(anyLong(), anyString(), anyString(), anyString(), anyLong());

            ProjectResponse response = projectService.createProject(request);

            assertThat(response.getType()).isEqualTo("CONSULTING");
            verify(factoryProvider).getFactory(Project.ProjectType.CONSULTING);
        }
    }

    @Nested
    @DisplayName("updateProject()")
    class UpdateProjectTests {

        @Test
        @DisplayName("✅ Actualiza nombre y descripción")
        void updateProject_existingId_updatesAndReturnsResponse() {
            UpdateProjectRequest request = new UpdateProjectRequest("Portal v2", "Nueva desc", null);
            Project updated = Project.builder().id(1L).name("Portal v2").description("Nueva desc")
                    .type(Project.ProjectType.SOFTWARE).status(Project.ProjectStatus.PLANNING)
                    .managerId(10L).createdAt(LocalDateTime.now()).build();

            when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
            when(projectRepository.save(any())).thenReturn(updated);

            ProjectResponse response = projectService.updateProject(1L, request);
            assertThat(response.getName()).isEqualTo("Portal v2");
        }

        @Test
        @DisplayName("❌ Lanza excepción si proyecto no existe")
        void updateProject_nonExistingId_throwsException() {
            when(projectRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> projectService.updateProject(99L, new UpdateProjectRequest("x", null, null)))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("no encontrado");
        }

        @Test
        @DisplayName("✅ Actualiza solo el estado")
        void updateStatus_existingProject_updatesStatus() {
            UpdateStatusRequest request = new UpdateStatusRequest(Project.ProjectStatus.IN_PROGRESS);
            when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
            when(projectRepository.save(any())).thenReturn(sampleProject);
            projectService.updateStatus(1L, request);
            verify(projectRepository).save(any(Project.class));
        }
    }

    @Nested
    @DisplayName("assignEmployee() / unassignEmployee()")
    class AssignEmployeeTests {

        @Test
        @DisplayName("✅ Asigna empleado a proyecto")
        void assignEmployee_existingProject_setsEmployeeFields() {
            AssignEmployeeRequest request = new AssignEmployeeRequest(5L, "Juan Pérez");
            when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
            when(projectRepository.save(any())).thenReturn(sampleProject);
            projectService.assignEmployee(1L, request);
            verify(projectRepository).save(any(Project.class));
        }

        @Test
        @DisplayName("✅ Desasigna empleado del proyecto")
        void unassignEmployee_existingProject_clearsFields() {
            sampleProject.setAssignedUserId(5L);
            when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
            when(projectRepository.save(any())).thenReturn(sampleProject);
            projectService.unassignEmployee(1L);
            verify(projectRepository).save(any(Project.class));
        }

        @Test
        @DisplayName("❌ Asignar a proyecto inexistente lanza excepción")
        void assignEmployee_nonExistingProject_throwsException() {
            when(projectRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> projectService.assignEmployee(99L, new AssignEmployeeRequest(1L, "Test")))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("deleteProject()")
    class DeleteProjectTests {

        @Test
        @DisplayName("✅ Elimina proyecto existente")
        void deleteProject_existingId_deletesSuccessfully() {
            when(projectRepository.existsById(1L)).thenReturn(true);
            doNothing().when(projectRepository).deleteById(1L);
            assertThatCode(() -> projectService.deleteProject(1L)).doesNotThrowAnyException();
            verify(projectRepository).deleteById(1L);
        }

        @Test
        @DisplayName("❌ Lanza excepción para proyecto inexistente")
        void deleteProject_nonExistingId_throwsException() {
            when(projectRepository.existsById(99L)).thenReturn(false);
            assertThatThrownBy(() -> projectService.deleteProject(99L))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("no encontrado");
            verify(projectRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("Consultas")
    class QueryTests {

        @Test
        @DisplayName("✅ getAllProjects retorna lista completa")
        void getAllProjects_returnsList() {
            when(projectRepository.findAll()).thenReturn(List.of(sampleProject));
            assertThat(projectService.getAllProjects()).hasSize(1);
        }

        @Test
        @DisplayName("✅ getProjectById retorna proyecto correcto")
        void getProjectById_existingId_returnsProject() {
            when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
            assertThat(projectService.getProjectById(1L).getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("✅ getProjectsByStatus filtra correctamente")
        void getProjectsByStatus_returnsFiltered() {
            when(projectRepository.findByStatus(Project.ProjectStatus.PLANNING)).thenReturn(List.of(sampleProject));
            assertThat(projectService.getProjectsByStatus(Project.ProjectStatus.PLANNING)).hasSize(1);
        }

        @Test
        @DisplayName("✅ getProjectsByAssignedUser usa índice de BD")
        void getProjectsByAssignedUser_usesRepository() {
            sampleProject.setAssignedUserId(5L);
            when(projectRepository.findByAssignedUserId(5L)).thenReturn(List.of(sampleProject));
            List<ProjectResponse> result = projectService.getProjectsByAssignedUser(5L);
            assertThat(result).hasSize(1);
            verify(projectRepository).findByAssignedUserId(5L);
        }

        @Test
        @DisplayName("✅ getTotalCount retorna conteo del repositorio")
        void getTotalCount_returnsCount() {
            when(projectRepository.count()).thenReturn(7L);
            assertThat(projectService.getTotalCount()).isEqualTo(7L);
        }
    }

    @Nested
    @DisplayName("Notas de avance")
    class NoteTests {

        @Test
        @DisplayName("✅ addNote crea nota PENDING")
        void addNote_existingProject_createsPending() {
            when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
            when(noteRepository.save(any(ProjectNote.class))).thenReturn(sampleNote);
            NoteResponse response = projectService.addNote(1L, new CreateNoteRequest("contenido"), 5L, "Test");
            assertThat(response.getStatus()).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("❌ addNote falla si proyecto no existe")
        void addNote_nonExistingProject_throwsException() {
            when(projectRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> projectService.addNote(99L, new CreateNoteRequest("x"), 1L, "User"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("✅ getNotesByProject retorna notas")
        void getNotesByProject_returnsNotes() {
            when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
            when(noteRepository.findByProjectId(1L)).thenReturn(List.of(sampleNote));
            assertThat(projectService.getNotesByProject(1L)).hasSize(1);
        }

        @Test
        @DisplayName("✅ reviewNote aprueba nota PENDING")
        void reviewNote_pendingNote_approvesAndSetsReviewer() {
            ReviewNoteRequest request = new ReviewNoteRequest(ProjectNote.NoteStatus.APPROVED, "Bien hecho");
            ProjectNote reviewed = ProjectNote.builder().id(1L).projectId(1L).authorId(5L)
                    .content("x").status(ProjectNote.NoteStatus.APPROVED).reviewerId(10L)
                    .reviewerName("Manager").reviewComment("Bien hecho").createdAt(LocalDateTime.now()).build();

            when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
            when(noteRepository.findById(1L)).thenReturn(Optional.of(sampleNote));
            when(noteRepository.save(any())).thenReturn(reviewed);

            NoteResponse response = projectService.reviewNote(1L, 1L, request, 10L, "Manager", "MANAGER");
            assertThat(response.getStatus()).isEqualTo("APPROVED");
        }

        @Test
        @DisplayName("❌ reviewNote falla si nota ya revisada")
        void reviewNote_alreadyReviewed_throwsException() {
            sampleNote.setStatus(ProjectNote.NoteStatus.APPROVED);
            when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
            when(noteRepository.findById(1L)).thenReturn(Optional.of(sampleNote));
            assertThatThrownBy(() -> projectService.reviewNote(1L, 1L,
                    new ReviewNoteRequest(ProjectNote.NoteStatus.REJECTED, null), 10L, "Manager", "MANAGER"))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("PENDING");
        }

        @Test
        @DisplayName("❌ reviewNote falla si nota no pertenece al proyecto")
        void reviewNote_noteFromDifferentProject_throwsException() {
            sampleNote.setProjectId(99L);
            when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
            when(noteRepository.findById(1L)).thenReturn(Optional.of(sampleNote));
            assertThatThrownBy(() -> projectService.reviewNote(1L, 1L,
                    new ReviewNoteRequest(ProjectNote.NoteStatus.APPROVED, null), 10L, "Manager", "MANAGER"))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("no pertenece");
        }
    }
}