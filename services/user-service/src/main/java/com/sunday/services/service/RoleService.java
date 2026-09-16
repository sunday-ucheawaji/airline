package com.sunday.services.service;

import com.sunday.common_lib.payload.request.AssignPermissionsRequest;
import com.sunday.common_lib.payload.request.RoleRequest;
import com.sunday.services.model.Permission;
import com.sunday.services.model.Role;

import java.util.List;

public interface RoleService {
    List<Role> getRoles();
    Role getRoleById(Long id);
    Role createRole(RoleRequest request);
    List<Permission> getPermissionsForRole(Long roleId);
    List<Permission> assignPermissionsToRole(Long roleId, AssignPermissionsRequest request);
    void unassignPermissionFromRole(Long roleId, Long permissionId);
}
