package com.sunday.services.client;

import com.sunday.common_lib.dto.PermissionDTO;
import com.sunday.common_lib.dto.RoleDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class RoleClientFallback implements RoleClient {

    @Override
    public List<PermissionDTO> getPermissionsForRole(UUID roleId) {
        return null;
    }

    @Override
    public RoleDTO getRoleById(UUID roleId) {
        return null;
    }

    @Override
    public RoleDTO getRoleByName(String name) {
        return null;
    }
}
