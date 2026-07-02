package com.innovatech.proyectos.patterns.factory;

import com.innovatech.proyectos.dto.ProjectDtos.CreateProjectRequest;
import com.innovatech.proyectos.model.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectFactoryProvider — instanciación de fábricas (Factory Method)")
class ProjectFactoryProviderTest {

    @Mock private SoftwareProjectFactory softwareFactory;
    @Mock private ConsultingProjectFactory consultingFactory;
    @Mock private InfrastructureProjectFactory infrastructureFactory;

    private ProjectFactoryProvider factoryProvider;

    @BeforeEach
    void setUp() {
        factoryProvider = new ProjectFactoryProvider(softwareFactory, consultingFactory, infrastructureFactory);
    }

    @Test
    @DisplayName("getFactory(SOFTWARE) retorna la instancia de SoftwareProjectFactory")
    void getFactory_software_returnsSoftwareFactory() {
        ProjectFactory result = factoryProvider.getFactory(Project.ProjectType.SOFTWARE);

        assertThat(result).isSameAs(softwareFactory);
    }

    @Test
    @DisplayName("getFactory(CONSULTING) retorna la instancia de ConsultingProjectFactory")
    void getFactory_consulting_returnsConsultingFactory() {
        ProjectFactory result = factoryProvider.getFactory(Project.ProjectType.CONSULTING);

        assertThat(result).isSameAs(consultingFactory);
    }

    @Test
    @DisplayName("getFactory(INFRASTRUCTURE) retorna la instancia de InfrastructureProjectFactory")
    void getFactory_infrastructure_returnsInfrastructureFactory() {
        ProjectFactory result = factoryProvider.getFactory(Project.ProjectType.INFRASTRUCTURE);

        assertThat(result).isSameAs(infrastructureFactory);
    }

    @Test
    @DisplayName("getFactory delega la creación en la fábrica concreta seleccionada (SOFTWARE)")
    void getFactory_software_delegatesProjectCreation() {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setName("Portal Retail");
        request.setType(Project.ProjectType.SOFTWARE);

        Project expected = Project.builder().name("Portal Retail").type(Project.ProjectType.SOFTWARE).build();
        when(softwareFactory.createProject(request)).thenReturn(expected);

        Project result = factoryProvider.getFactory(Project.ProjectType.SOFTWARE).createProject(request);

        assertThat(result).isSameAs(expected);
    }

    @Test
    @DisplayName("getFactory(null) lanza IllegalArgumentException (branch por defecto del switch exhaustivo)")
    void getFactory_nullType_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> factoryProvider.getFactory(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}