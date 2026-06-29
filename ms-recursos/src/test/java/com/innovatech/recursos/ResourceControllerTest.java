package com.innovatech.recursos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innovatech.recursos.config.SecurityConfig;
import com.innovatech.recursos.controller.ResourceController;
import com.innovatech.recursos.dto.ResourceDtos.CreateResourceRequest;
import com.innovatech.recursos.dto.ResourceDtos.ResourceResponse;
import com.innovatech.recursos.dto.ResourceDtos.UpdateAvailabilityRequest;
import com.innovatech.recursos.model.Resource;
import com.innovatech.recursos.service.ResourceService;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        value = ResourceController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@DisplayName("ResourceController — WebMvcTest")
class ResourceControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private ResourceService resourceService;

    private ResourceResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleResponse = ResourceResponse.builder()
                .id(1L).name("Ana García").email("ana@innovatech.com")
                .department("Engineering").role("DEVELOPER")
                .available(true).createdAt(LocalDateTime.now()).build();
    }

    // ── POST /api/resources ───────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/resources → 201 con datos válidos")
    void createResource_validRequest_returns201() throws Exception {
        when(resourceService.createResource(any())).thenReturn(sampleResponse);

        CreateResourceRequest req = new CreateResourceRequest();
        req.setName("Ana García");
        req.setEmail("ana@innovatech.com");
        req.setDepartment("Engineering");
        req.setRole(Resource.ResourceRole.DEVELOPER);

        mockMvc.perform(post("/api/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("ana@innovatech.com"));
    }

    @Test
    @DisplayName("POST /api/resources → 409 si email duplicado")
    void createResource_duplicateEmail_returns400() throws Exception {
        when(resourceService.createResource(any()))
                .thenThrow(new IllegalArgumentException("Ya existe un recurso con el email: ana@innovatech.com"));

        CreateResourceRequest req = new CreateResourceRequest();
        req.setName("Ana García");
        req.setEmail("ana@innovatech.com");
        req.setDepartment("Engineering");

        mockMvc.perform(post("/api/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/resources ────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/resources → 200 con lista")
    void getAll_returns200WithList() throws Exception {
        when(resourceService.getAllResources()).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/resources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Ana García"))
                .andExpect(jsonPath("$[0].available").value(true));
    }

    @Test
    @DisplayName("GET /api/resources/available → 200")
    void getAvailable_returns200() throws Exception {
        when(resourceService.getAvailableResources()).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/resources/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].available").value(true));
    }

    @Test
    @DisplayName("GET /api/resources/department/{dept} → 200")
    void getByDepartment_returns200() throws Exception {
        when(resourceService.getByDepartment("Engineering")).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/resources/department/Engineering"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].department").value("Engineering"));
    }

    @Test
    @DisplayName("GET /api/resources/{id} → 200")
    void getById_existing_returns200() throws Exception {
        when(resourceService.getById(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/resources/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("GET /api/resources/{id} → 400 si no existe")
    void getById_notFound_returns400() throws Exception {
        when(resourceService.getById(anyLong()))
                .thenThrow(new IllegalArgumentException("Recurso no encontrado"));

        mockMvc.perform(get("/api/resources/99"))
                .andExpect(status().isBadRequest());
    }

    // ── PATCH /api/resources/{id}/availability ────────────────────────────────

    @Test
    @DisplayName("PATCH /api/resources/{id}/availability → 200")
    void updateAvailability_returns200() throws Exception {
        ResourceResponse updated = ResourceResponse.builder()
                .id(1L).name("Ana García").email("ana@innovatech.com")
                .department("Engineering").role("DEVELOPER")
                .available(false).createdAt(LocalDateTime.now()).build();
        when(resourceService.updateAvailability(eq(1L), any())).thenReturn(updated);

        UpdateAvailabilityRequest req = new UpdateAvailabilityRequest(false);

        mockMvc.perform(patch("/api/resources/1/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }
}