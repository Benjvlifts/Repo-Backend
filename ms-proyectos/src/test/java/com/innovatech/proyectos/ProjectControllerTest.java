package com.innovatech.proyectos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innovatech.proyectos.config.JwtAuthFilter;
import com.innovatech.proyectos.config.JwtExtractor;
import com.innovatech.proyectos.config.SecurityConfig;
import com.innovatech.proyectos.controller.ProjectController;
import com.innovatech.proyectos.dto.ProjectDtos.AssignEmployeeRequest;
import com.innovatech.proyectos.dto.ProjectDtos.CreateProjectRequest;
import com.innovatech.proyectos.dto.ProjectDtos.NoteResponse;
import com.innovatech.proyectos.dto.ProjectDtos.ProjectResponse;
import com.innovatech.proyectos.dto.ProjectDtos.ReviewNoteRequest;
import com.innovatech.proyectos.dto.ProjectDtos.UpdateStatusRequest;
import com.innovatech.proyectos.model.Project;
import com.innovatech.proyectos.model.ProjectNote;
import com.innovatech.proyectos.service.ProjectService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        value = ProjectController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthFilter.class}
        )
)
@DisplayName("ProjectController — WebMvcTest")
class ProjectControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private ProjectService projectService;
    @MockBean  private JwtExtractor jwtExtractor;

    private static final String FAKE_BEARER = "Bearer fake-token";

    private ProjectResponse buildResponse(Long id, String name) {
        return ProjectResponse.builder()
                .id(id).name(name).type("SOFTWARE")
                .status("PLANNING").managerId(1L).build();
    }

    // ── Health (endpoint público, sin JWT) ────────────────────────────────────

    @Test
    @DisplayName("GET /api/projects/health → 200")
    void health_returns200() throws Exception {
        mockMvc.perform(get("/api/projects/health"))
                .andExpect(status().isOk());
    }

    // ── POST /api/projects ────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/projects → 201 cuando rol es ADMIN")
    void createProject_asAdmin_returns201() throws Exception {
        when(jwtExtractor.isAdmin(anyString())).thenReturn(true);
        when(projectService.createProject(any())).thenReturn(buildResponse(1L, "Portal Retail"));

        CreateProjectRequest req = new CreateProjectRequest();
        req.setName("Portal Retail");
        req.setType(Project.ProjectType.SOFTWARE);

        mockMvc.perform(post("/api/projects")
                        .header("Authorization", FAKE_BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Portal Retail"));
    }

    @Test
    @DisplayName("POST /api/projects → 403 cuando NO es ADMIN")
    void createProject_notAdmin_returns403() throws Exception {
        when(jwtExtractor.isAdmin(anyString())).thenReturn(false);

        CreateProjectRequest req = new CreateProjectRequest();
        req.setName("Proyecto B");
        req.setType(Project.ProjectType.CONSULTING);

        mockMvc.perform(post("/api/projects")
                        .header("Authorization", FAKE_BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // ── GET /api/projects ─────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/projects → 200 para ADMIN (todos los proyectos)")
    void getAllProjects_asAdminOrManager_returns200() throws Exception {
        when(jwtExtractor.isEmployee(anyString())).thenReturn(false);
        when(projectService.getAllProjects())
                .thenReturn(List.of(buildResponse(1L, "A"), buildResponse(2L, "B")));

        mockMvc.perform(get("/api/projects").header("Authorization", FAKE_BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/projects → 200 para EMPLOYEE (solo sus proyectos)")
    void getAllProjects_asEmployee_returnsOwn() throws Exception {
        when(jwtExtractor.isEmployee(anyString())).thenReturn(true);
        when(jwtExtractor.extractUserId(anyString())).thenReturn(7L);
        when(projectService.getProjectsByAssignedUser(7L))
                .thenReturn(List.of(buildResponse(3L, "Mi Proyecto")));

        mockMvc.perform(get("/api/projects").header("Authorization", FAKE_BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Mi Proyecto"));
    }

    @Test
    @DisplayName("GET /api/projects?status=PLANNING → 200 filtrado por estado")
    void getAllProjects_filterByStatus_returns200() throws Exception {
        when(jwtExtractor.isEmployee(anyString())).thenReturn(false);
        when(projectService.getProjectsByStatus(Project.ProjectStatus.PLANNING))
                .thenReturn(List.of(buildResponse(1L, "Nuevo")));

        mockMvc.perform(get("/api/projects")
                        .param("status", "PLANNING")
                        .header("Authorization", FAKE_BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PLANNING"));
    }

    // ── GET /api/projects/{id} ────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/projects/{id} → 200 cuando el solicitante es ADMIN")
    void getProjectById_asAdmin_returns200() throws Exception {
        when(jwtExtractor.extractUserId(anyString())).thenReturn(1L);
        when(jwtExtractor.isAdmin(anyString())).thenReturn(true);
        when(projectService.getProjectById(1L)).thenReturn(buildResponse(1L, "Portal Retail"));

        mockMvc.perform(get("/api/projects/1").header("Authorization", FAKE_BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("GET /api/projects/{id} → 200 cuando el solicitante es el manager asignado")
    void getProjectById_asAssignedManager_returns200() throws Exception {
        when(jwtExtractor.extractUserId(anyString())).thenReturn(9L);
        when(jwtExtractor.isAdmin(anyString())).thenReturn(false);
        when(projectService.getProjectById(1L)).thenReturn(buildResponse(1L, "Portal Retail"));

        mockMvc.perform(get("/api/projects/1").header("Authorization", FAKE_BEARER))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/projects/{id} → 403 cuando NO es ADMIN ni el manager asignado")
    void getProjectById_notAdminNorAssignedManager_returns403() throws Exception {
        when(jwtExtractor.extractUserId(anyString())).thenReturn(99L);
        when(jwtExtractor.isAdmin(anyString())).thenReturn(false);
        when(projectService.getProjectById(1L)).thenReturn(buildResponse(1L, "Portal Retail"));

        mockMvc.perform(get("/api/projects/1").header("Authorization", FAKE_BEARER))
                .andExpect(status().isForbidden());
    }

    // ── PATCH /api/projects/{id}/status ───────────────────────────────────────

    @Test
    @DisplayName("PATCH /api/projects/{id}/status → 200 cuando rol es ADMIN")
    void updateStatus_asAdmin_returns200() throws Exception {
        when(jwtExtractor.isAdmin(anyString())).thenReturn(true);
        when(projectService.updateStatus(eq(1L), any()))
                .thenReturn(buildResponse(1L, "Portal Retail"));

        UpdateStatusRequest req = new UpdateStatusRequest();
        req.setStatus(Project.ProjectStatus.IN_PROGRESS);

        mockMvc.perform(patch("/api/projects/1/status")
                        .header("Authorization", FAKE_BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /api/projects/{id}/status → 403 cuando NO es ADMIN")
    void updateStatus_notAdmin_returns403() throws Exception {
        when(jwtExtractor.isAdmin(anyString())).thenReturn(false);

        UpdateStatusRequest req = new UpdateStatusRequest();
        req.setStatus(Project.ProjectStatus.IN_PROGRESS);

        mockMvc.perform(patch("/api/projects/1/status")
                        .header("Authorization", FAKE_BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // ── PATCH /api/projects/{id}/assign ───────────────────────────────────────

    @Test
    @DisplayName("PATCH /api/projects/{id}/assign → 200 cuando rol es ADMIN o MANAGER")
    void assignEmployee_asAdminOrManager_returns200() throws Exception {
        when(jwtExtractor.isAdminOrManager(anyString())).thenReturn(true);
        when(projectService.assignEmployee(eq(1L), any()))
                .thenReturn(buildResponse(1L, "Portal Retail"));

        AssignEmployeeRequest req = new AssignEmployeeRequest();
        req.setEmployeeId(5L);
        req.setEmployeeName("Ignacio Muñoz");

        mockMvc.perform(patch("/api/projects/1/assign")
                        .header("Authorization", FAKE_BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /api/projects/{id}/assign → 403 cuando NO es ADMIN ni MANAGER")
    void assignEmployee_notAdminNorManager_returns403() throws Exception {
        when(jwtExtractor.isAdminOrManager(anyString())).thenReturn(false);

        AssignEmployeeRequest req = new AssignEmployeeRequest();
        req.setEmployeeId(5L);
        req.setEmployeeName("Ignacio Muñoz");

        mockMvc.perform(patch("/api/projects/1/assign")
                        .header("Authorization", FAKE_BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // ── DELETE /api/projects/{id} ─────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/projects/{id} → 204 cuando rol es ADMIN")
    void deleteProject_asAdmin_returns204() throws Exception {
        when(jwtExtractor.isAdmin(anyString())).thenReturn(true);

        mockMvc.perform(delete("/api/projects/1").header("Authorization", FAKE_BEARER))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/projects/{id} → 403 cuando NO es ADMIN")
    void deleteProject_notAdmin_returns403() throws Exception {
        when(jwtExtractor.isAdmin(anyString())).thenReturn(false);

        mockMvc.perform(delete("/api/projects/1").header("Authorization", FAKE_BEARER))
                .andExpect(status().isForbidden());
    }

    // ── PATCH /api/projects/{id}/notes/{noteId}/review ───────────────────────

    @Test
    @DisplayName("PATCH /api/projects/{id}/notes/{noteId}/review → 200 cuando rol es ADMIN o MANAGER")
    void reviewNote_asAdminOrManager_returns200() throws Exception {
        when(jwtExtractor.isAdminOrManager(anyString())).thenReturn(true);
        when(jwtExtractor.extractUserId(anyString())).thenReturn(1L);
        when(jwtExtractor.extractSubject(anyString())).thenReturn("admin@innovatech.cl");
        when(jwtExtractor.extractRole(anyString())).thenReturn("ADMIN");
        when(projectService.reviewNote(eq(1L), eq(10L), any(), eq(1L), eq("admin@innovatech.cl"), eq("ADMIN")))
                .thenReturn(NoteResponse.builder()
                        .id(10L).projectId(1L).status("APPROVED").build());

        ReviewNoteRequest req = new ReviewNoteRequest();
        req.setStatus(ProjectNote.NoteStatus.APPROVED);
        req.setReviewComment("Buen avance");

        mockMvc.perform(patch("/api/projects/1/notes/10/review")
                        .header("Authorization", FAKE_BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("PATCH /api/projects/{id}/notes/{noteId}/review → 403 cuando NO es ADMIN ni MANAGER")
    void reviewNote_notAdminNorManager_returns403() throws Exception {
        when(jwtExtractor.isAdminOrManager(anyString())).thenReturn(false);

        ReviewNoteRequest req = new ReviewNoteRequest();
        req.setStatus(ProjectNote.NoteStatus.REJECTED);

        mockMvc.perform(patch("/api/projects/1/notes/10/review")
                        .header("Authorization", FAKE_BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }
}