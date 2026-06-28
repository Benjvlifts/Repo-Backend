package com.innovatech.recursos.repository;

import com.innovatech.recursos.model.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaResourceRepository extends JpaRepository<Resource, Long>, IResourceRepository {
    @Override List<Resource> findByAvailable(boolean available);
    @Override List<Resource> findByDepartment(String department);
    @Override List<Resource> findByRole(Resource.ResourceRole role);
    @Override List<Resource> findByAssignedProjectId(Long assignedProjectId); // FIX Bug 3
    @Override boolean existsByEmail(String email);
}