package com.sunday.services.service;

import com.sunday.common_lib.payload.request.PermissionRequest;
import com.sunday.services.model.Permission;
import com.sunday.services.model.Role;

import java.util.List;

public interface PermissionService {
    List<Permission> getPermissions();
    Permission createPermission(PermissionRequest request);
    List<Role> getRolesForPermission(Long permissionId);
}
