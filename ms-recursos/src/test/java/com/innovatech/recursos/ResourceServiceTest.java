package com.innovatech.recursos;

import com.innovatech.recursos.dto.ResourceDtos.*;
import com.innovatech.recursos.model.Resource;
import com.innovatech.recursos.repository.IResourceRepository;
import com.innovatech.recursos.service.ResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @Mock private IResourceRepository resourceRepository;
    @InjectMocks private ResourceService resourceService;

    private Resource sampleResource;

    @BeforeEach
    void setUp() {
        sampleResource = Resource.builder()
                .id(1L).name("Servidor AWS").department("IT").role(Resource.ResourceRole.DEVELOPER).available(true).build();
    }

    @Test
    void createResource_savesAndReturnsResponse() {
        CreateResourceRequest req = new CreateResourceRequest("Servidor AWS", "aws@test.com", "IT", Resource.ResourceRole.DEVELOPER, "Java");
        when(resourceRepository.existsByEmail(anyString())).thenReturn(false);
        when(resourceRepository.save(any(Resource.class))).thenReturn(sampleResource);

        ResourceResponse res = resourceService.createResource(req);

        assertThat(res.getName()).isEqualTo("Servidor AWS");
        verify(resourceRepository).save(any(Resource.class));
    }

    @Test
    void getAllResources_returnsList() {
        when(resourceRepository.findAll()).thenReturn(List.of(sampleResource));
        List<ResourceResponse> res = resourceService.getAllResources();
        assertThat(res).hasSize(1);
    }
}