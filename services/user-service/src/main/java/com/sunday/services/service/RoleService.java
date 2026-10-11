package com.sunday.services.service;
import java.util.UUID;

import com.sunday.common_lib.payload.request.AssignPermissionsRequest;
import com.sunday.common_lib.payload.request.RoleRequest;
import com.sunday.common_lib.payload.response.ReviewerResponse;
import com.sunday.services.model.Permission;
import com.sunday.services.model.Role;

import java.util.Collection;
import java.util.List;

public interface RoleService {
    List<Role> getRoles();
    Role getRoleById(UUID id);
    Role getRoleByName(String name);
    Role createRole(RoleRequest request);
    List<Permission> getPermissionsForRole(UUID roleId);
    List<Permission> assignPermissionsToRole(UUID roleId, AssignPermissionsRequest request);
    void unassignPermissionFromRole(UUID roleId, UUID permissionId);
    List<Role> getPlatformRolesForUser(UUID userId);

    /** Active users who hold the given platform role — used to pick reviewers for a case or stage. */
    List<ReviewerResponse> getReviewersByRole(String roleName);
    void assignPlatformRoleToUser(UUID roleId, UUID userId, UUID grantorUserId, Collection<String> grantorRoles);
    void unassignPlatformRoleFromUser(UUID roleId, UUID userId, Collection<String> grantorRoles);

}
