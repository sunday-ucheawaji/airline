package com.sunday.services.client;

import com.sunday.common_lib.dto.PermissionDTO;
import com.sunday.common_lib.dto.RoleDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "user-service", fallbackFactory = RoleClientFallbackFactory.class)
public interface RoleClient {

    @GetMapping("/api/roles/{roleId}/permissions")
    List<PermissionDTO> getPermissionsForRole(@PathVariable UUID roleId);

    @GetMapping("/api/roles/{roleId}")
    RoleDTO getRoleById(@PathVariable UUID roleId);

    @GetMapping("/api/roles/by-name/{name}")
    RoleDTO getRoleByName(@PathVariable String name);
}
