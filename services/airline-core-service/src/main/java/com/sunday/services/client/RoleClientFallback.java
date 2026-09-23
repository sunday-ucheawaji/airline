package com.sunday.services.client;

import com.sunday.common_lib.dto.PermissionDTO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RoleClientFallback implements RoleClient {

    @Override
    public List<PermissionDTO> getPermissionsForRole(Long roleId) {
        return null;
    }
}
