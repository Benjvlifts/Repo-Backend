package com.innovatech.proyectos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innovatech.proyectos.config.JwtAuthFilter;
import com.innovatech.proyectos.config.JwtExtractor;
import com.innovatech.proyectos.config.SecurityConfig;
import com.innovatech.proyectos.controller.ProjectController;
import com.innovatech.proyectos.dto.ProjectDtos.CreateProjectRequest;
import com.innovatech.proyectos.dto.ProjectDtos.ProjectResponse;
import com.innovatech.proyectos.model.Project;
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
}