package com.sunday.services.repository;

import com.sunday.services.enums.RoleScope;
import com.sunday.services.enums.RoleStatus;
import com.sunday.services.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Role findByName(String name);

    List<Role> findByScopeAndStatus(RoleScope scope, RoleStatus status);
}
