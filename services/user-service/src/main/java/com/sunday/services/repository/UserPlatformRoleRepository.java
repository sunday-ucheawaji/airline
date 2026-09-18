package com.sunday.services.repository;

import com.sunday.services.model.UserPlatformRole;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserPlatformRoleRepository extends JpaRepository<UserPlatformRole, Long> {

    @EntityGraph(attributePaths = "role")
    List<UserPlatformRole> findByUserId(Long userId);

    Optional<UserPlatformRole> findByUserIdAndRoleId(Long userId, Long roleId);

    boolean existsByUserIdAndRoleId(Long userId, Long roleId);
}
