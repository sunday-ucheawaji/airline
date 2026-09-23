package com.sunday.services.service.impl;

import com.sunday.common_lib.dto.PermissionDTO;
import com.sunday.services.client.RoleClient;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Deliberately its own bean, not a method on AirlineServiceImpl: Spring's @Cacheable proxy is
 * bypassed on self-invocation, so calling this method on `this` from within AirlineServiceImpl
 * would silently never hit the cache. Cardinality here is tiny system-wide — every airline's
 * OWNER membership points at the same Role row — so this cache is expected to have a very high
 * hit rate regardless of how many users/airlines call through it.
 */
@Service
@RequiredArgsConstructor
public class RolePermissionCacheService {

    private final RoleClient roleClient;

    @Cacheable(cacheNames = "rolePermissions", key = "#roleId")
    public List<String> getPermissionNamesForRole(Long roleId) {
        List<PermissionDTO> permissions = roleClient.getPermissionsForRole(roleId);
        if (permissions == null) {
            throw new RuntimeException("user-service unavailable");
        }
        return permissions.stream().map(PermissionDTO::getName).toList();
    }
}
