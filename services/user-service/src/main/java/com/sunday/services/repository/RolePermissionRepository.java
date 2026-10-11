package com.sunday.services.repository;

import com.sunday.services.model.Role;
import com.sunday.services.model.RolePermission;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RolePermissionRepository extends JpaRepository<RolePermission, UUID> {

    @EntityGraph(attributePaths = "permission")
    List<RolePermission> findByRoleId(UUID roleId);

    @EntityGraph(attributePaths = "role")
    List<RolePermission> findByPermissionId(UUID permissionId);

    @EntityGraph(attributePaths = "permission")
    List<RolePermission> findByRoleIn(Collection<Role> roles);

    Optional<RolePermission> findByRoleIdAndPermissionId(UUID roleId, UUID permissionId);

    boolean existsByRoleIdAndPermissionId(UUID roleId, UUID permissionId);
}
