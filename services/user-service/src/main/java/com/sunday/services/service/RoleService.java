package com.sunday.services.service;

import com.sunday.common_lib.payload.request.AssignPermissionsRequest;
import com.sunday.common_lib.payload.request.RoleRequest;
import com.sunday.common_lib.payload.response.ReviewerResponse;
import com.sunday.services.model.Permission;
import com.sunday.services.model.Role;

import java.util.Collection;
import java.util.List;

public interface RoleService {
    List<Role> getRoles();
    Role getRoleById(Long id);
    Role createRole(RoleRequest request);
    List<Permission> getPermissionsForRole(Long roleId);
    List<Permission> assignPermissionsToRole(Long roleId, AssignPermissionsRequest request);
    void unassignPermissionFromRole(Long roleId, Long permissionId);
    List<Role> getPlatformRolesForUser(Long userId);

    /** Active users who hold the given platform role — used to pick reviewers for a case or stage. */
    List<ReviewerResponse> getReviewersByRole(String roleName);
    void assignPlatformRoleToUser(Long roleId, Long userId, Long grantorUserId, Collection<String> grantorRoles);
    void unassignPlatformRoleFromUser(Long roleId, Long userId, Collection<String> grantorRoles);

}
