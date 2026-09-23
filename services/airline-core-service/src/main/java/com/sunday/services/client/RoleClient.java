package com.sunday.services.client;

import com.sunday.common_lib.dto.PermissionDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "user-service", fallback = RoleClientFallback.class)
public interface RoleClient {

    @GetMapping("/api/roles/{roleId}/permissions")
    List<PermissionDTO> getPermissionsForRole(@PathVariable Long roleId);
}
