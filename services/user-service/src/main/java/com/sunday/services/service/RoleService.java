package com.sunday.services.service;

import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.payload.request.AssignPermissionsRequest;
import com.sunday.common_lib.payload.request.RoleRequest;
import com.sunday.services.model.Permission;
import com.sunday.services.model.Role;

import java.util.List;

public interface RoleService {
    List<Role> getRoles() throws ResourceNotFoundException;
    Role getRoleById(Long id) throws ResourceNotFoundException;
    Role createRole(RoleRequest request);
    List<Permission> getPermissionsForRole(Long roleId) throws ResourceNotFoundException;
    List<Permission> assignPermissionsToRole(Long roleId, AssignPermissionsRequest request) throws ResourceNotFoundException;
    void unassignPermissionFromRole(Long roleId, Long permissionId) throws ResourceNotFoundException;
}
