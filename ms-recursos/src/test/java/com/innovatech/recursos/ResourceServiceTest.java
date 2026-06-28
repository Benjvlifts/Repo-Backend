package com.innovatech.recursos;

import com.innovatech.recursos.dto.ResourceDtos.*;
import com.innovatech.recursos.model.Resource;
import com.innovatech.recursos.repository.IResourceRepository;
import com.innovatech.recursos.service.ResourceService;
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
@DisplayName("ResourceService — Pruebas Unitarias")
class ResourceServiceTest {

    @Mock private IResourceRepository resourceRepository;
    @InjectMocks private ResourceService resourceService;

    private Resource sampleResource;

    @BeforeEach
    void setUp() {
        sampleResource = Resource.builder()
                .id(1L).name("Ana López").email("ana@test.cl").department("IT")
                .role(Resource.ResourceRole.DEVELOPER).available(true)
                .skills("Java,Spring").createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("createResource()")
    class CreateResourceTests {

        @Test
        @DisplayName("✅ Crea recurso y retorna response")
        void createResource_savesAndReturnsResponse() {
            CreateResourceRequest req = new CreateResourceRequest("Ana López", "ana@test.cl", "IT", Resource.ResourceRole.DEVELOPER, "Java");
            when(resourceRepository.existsByEmail("ana@test.cl")).thenReturn(false);
            when(resourceRepository.save(any(Resource.class))).thenReturn(sampleResource);

            ResourceResponse res = resourceService.createResource(req);

            assertThat(res.getName()).isEqualTo("Ana López");
            assertThat(res.isAvailable()).isTrue();
            verify(resourceRepository).save(any(Resource.class));
        }

        @Test
        @DisplayName("✅ Asigna rol DEVELOPER si no se especifica")
        void createResource_noRole_assignsDeveloper() {
            CreateResourceRequest req = new CreateResourceRequest("Test", "test@test.cl", "IT", null, null);
            when(resourceRepository.existsByEmail(anyString())).thenReturn(false);
            when(resourceRepository.save(any())).thenReturn(sampleResource);
            resourceService.createResource(req);
            verify(resourceRepository).save(argThat(r -> r.getRole() == Resource.ResourceRole.DEVELOPER));
        }

        @Test
        @DisplayName("❌ Lanza excepción si email ya existe")
        void createResource_duplicateEmail_throwsException() {
            CreateResourceRequest req = new CreateResourceRequest("Test", "ana@test.cl", "IT", null, null);
            when(resourceRepository.existsByEmail("ana@test.cl")).thenReturn(true);
            assertThatThrownBy(() -> resourceService.createResource(req))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("email");
        }
    }

    @Nested
    @DisplayName("Consultas")
    class QueryTests {

        @Test
        @DisplayName("✅ getAllResources retorna lista")
        void getAllResources_returnsList() {
            when(resourceRepository.findAll()).thenReturn(List.of(sampleResource));
            assertThat(resourceService.getAllResources()).hasSize(1);
        }

        @Test
        @DisplayName("✅ getAvailableResources retorna solo disponibles")
        void getAvailableResources_returnsAvailable() {
            when(resourceRepository.findByAvailable(true)).thenReturn(List.of(sampleResource));
            assertThat(resourceService.getAvailableResources()).hasSize(1);
        }

        @Test
        @DisplayName("✅ getByDepartment filtra correctamente")
        void getByDepartment_returnsFiltered() {
            when(resourceRepository.findByDepartment("IT")).thenReturn(List.of(sampleResource));
            assertThat(resourceService.getByDepartment("IT")).hasSize(1);
        }

        @Test
        @DisplayName("✅ getById retorna recurso correcto")
        void getById_existingId_returnsResource() {
            when(resourceRepository.findById(1L)).thenReturn(Optional.of(sampleResource));
            assertThat(resourceService.getById(1L).getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("❌ getById lanza excepción si no existe")
        void getById_nonExisting_throwsException() {
            when(resourceRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> resourceService.getById(99L))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("no encontrado");
        }
    }

    @Nested
    @DisplayName("Modificaciones")
    class MutationTests {

        @Test
        @DisplayName("✅ updateAvailability cambia disponibilidad")
        void updateAvailability_changesFlag() {
            when(resourceRepository.findById(1L)).thenReturn(Optional.of(sampleResource));
            when(resourceRepository.save(any())).thenReturn(sampleResource);
            resourceService.updateAvailability(1L, new UpdateAvailabilityRequest(false));
            verify(resourceRepository).save(argThat(r -> !r.isAvailable()));
        }

        @Test
        @DisplayName("✅ assignToProject marca recurso como no disponible")
        void assignToProject_setsProjectAndUnavailable() {
            when(resourceRepository.findById(1L)).thenReturn(Optional.of(sampleResource));
            when(resourceRepository.save(any())).thenReturn(sampleResource);
            resourceService.assignToProject(1L, 10L, "Proyecto Alpha");
            verify(resourceRepository).save(argThat(r -> !r.isAvailable() && r.getAssignedProjectId().equals(10L)));
        }

        @Test
        @DisplayName("✅ deleteResource elimina recurso existente")
        void deleteResource_existingId_deletes() {
            when(resourceRepository.findById(1L)).thenReturn(Optional.of(sampleResource));
            doNothing().when(resourceRepository).deleteById(1L);
            assertThatCode(() -> resourceService.deleteResource(1L)).doesNotThrowAnyException();
            verify(resourceRepository).deleteById(1L);
        }

        @Test
        @DisplayName("❌ deleteResource lanza excepción si no existe")
        void deleteResource_nonExisting_throwsException() {
            when(resourceRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> resourceService.deleteResource(99L))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("no encontrado");
            verify(resourceRepository, never()).deleteById(any());
        }
    }
}