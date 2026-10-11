package com.sunday.services.repository;

import com.sunday.services.model.UserPlatformRole;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

import java.util.List;
import java.util.Optional;

public interface UserPlatformRoleRepository extends JpaRepository<UserPlatformRole, UUID> {

    @EntityGraph(attributePaths = "role")
    List<UserPlatformRole> findByUserId(UUID userId);

    @EntityGraph(attributePaths = "user")
    List<UserPlatformRole> findByRoleName(String roleName);

    Optional<UserPlatformRole> findByUserIdAndRoleId(UUID userId, UUID roleId);

    boolean existsByUserIdAndRoleId(UUID userId, UUID roleId);
}
