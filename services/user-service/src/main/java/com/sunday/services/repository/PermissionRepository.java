package com.sunday.services.repository;

import com.sunday.services.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    Permission findByName(String name);
}
