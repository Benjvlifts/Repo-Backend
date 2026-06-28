package com.innovatech.proyectos.patterns.factory;

import com.innovatech.proyectos.model.Project.ProjectType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Proveedor de fábricas de proyectos.
 *
 * PATRÓN FACTORY METHOD: Selecciona y retorna la fábrica concreta adecuada
 * según el tipo de proyecto solicitado. El servicio usa este proveedor
 * en lugar de instanciar fábricas directamente.
 *
 * @author Benjamin Valdes, Ignacio Munoz
 */
@Component
@RequiredArgsConstructor
public class ProjectFactoryProvider {

    private final SoftwareProjectFactory softwareFactory;
    private final ConsultingProjectFactory consultingFactory;
    private final InfrastructureProjectFactory infrastructureFactory;

    /**
     * Retorna la fábrica apropiada para el tipo de proyecto indicado.
     *
     * @param type tipo de proyecto
     * @return fábrica concreta
     * @throws IllegalArgumentException si el tipo no está soportado
     */
    public ProjectFactory getFactory(ProjectType type) {
        return switch (type) {
            case SOFTWARE       -> softwareFactory;
            case CONSULTING     -> consultingFactory;
            case INFRASTRUCTURE -> infrastructureFactory;
        };
    }
}