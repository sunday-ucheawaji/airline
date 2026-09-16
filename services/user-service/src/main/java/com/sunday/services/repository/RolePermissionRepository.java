package com.sunday.services.repository;

import com.sunday.services.model.RolePermission;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    @EntityGraph(attributePaths = "permission")
    List<RolePermission> findByRoleId(Long roleId);

    @EntityGraph(attributePaths = "role")
    List<RolePermission> findByPermissionId(Long permissionId);

    Optional<RolePermission> findByRoleIdAndPermissionId(Long roleId, Long permissionId);

    boolean existsByRoleIdAndPermissionId(Long roleId, Long permissionId);
}
