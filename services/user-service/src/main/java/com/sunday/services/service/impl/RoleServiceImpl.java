package com.sunday.services.service.impl;

import com.sunday.common_lib.constants.PlatformRoles;
import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.OperationNotPermittedException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.exception.ServiceUnavailableException;
import com.sunday.common_lib.payload.request.AssignPermissionsRequest;
import com.sunday.common_lib.payload.request.RoleRequest;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.client.MembershipClient;
import com.sunday.services.enums.RoleScope;
import com.sunday.services.model.Permission;
import com.sunday.services.model.Role;
import com.sunday.services.model.RolePermission;
import com.sunday.services.model.User;
import com.sunday.services.model.UserPlatformRole;
import com.sunday.services.repository.PermissionRepository;
import com.sunday.services.repository.RolePermissionRepository;
import com.sunday.services.repository.RoleRepository;
import com.sunday.services.repository.UserPlatformRoleRepository;
import com.sunday.services.repository.UserRepository;
import com.sunday.services.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRepository userRepository;
    private final UserPlatformRoleRepository userPlatformRoleRepository;
    private final MembershipClient membershipClient;

    @Override
    public List<Role> getRoles() {
        return roleRepository.findAll();
    }

    @Override
    public Role getRoleById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.ROLE_NOT_FOUND_BY_ID, id)));
    }

    @Override
    public Role createRole(RoleRequest request) {
        if (roleRepository.findByName(request.getName()) != null) {
            throw new OperationNotPermittedException(String.format(ErrorMessageUtil.ROLE_ALREADY_EXISTS, request.getName()));
        }

        Role role = new Role();
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        role.setScope(request.getScope() != null ? RoleScope.valueOf(request.getScope()) : RoleScope.AIRLINE);

        return roleRepository.save(role);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Permission> getPermissionsForRole(Long roleId) {
        getRoleById(roleId);

        return rolePermissionRepository.findByRoleId(roleId).stream()
                .map(RolePermission::getPermission)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<Permission> assignPermissionsToRole(Long roleId, AssignPermissionsRequest request) {
        Role role = getRoleById(roleId);

        for (Long permissionId : request.getPermissionIds()) {
            Permission permission = permissionRepository.findById(permissionId)
                    .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.PERMISSION_NOT_FOUND_BY_ID, permissionId)));

            if (!rolePermissionRepository.existsByRoleIdAndPermissionId(roleId, permissionId)) {
                RolePermission rolePermission = new RolePermission();
                rolePermission.setRole(role);
                rolePermission.setPermission(permission);
                rolePermissionRepository.save(rolePermission);
            }
        }

        return getPermissionsForRole(roleId);
    }

    @Override
    public void unassignPermissionFromRole(Long roleId, Long permissionId) {
        getRoleById(roleId);

        RolePermission rolePermission = rolePermissionRepository
                .findByRoleIdAndPermissionId(roleId, permissionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.PERMISSION_NOT_ASSIGNED_TO_ROLE, permissionId, roleId)));

        rolePermissionRepository.delete(rolePermission);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> getPlatformRolesForUser(Long userId) {
        getUserOrThrow(userId);

        return userPlatformRoleRepository.findByUserId(userId).stream()
                .map(UserPlatformRole::getRole)
                .collect(Collectors.toList());
    }

    @Override
    public void assignPlatformRoleToUser(Long roleId, Long userId, Long grantorUserId, Collection<String> grantorRoles) {
        Role role = getRoleById(roleId);
        if (role.getScope() != RoleScope.PLATFORM) {
            throw new OperationNotPermittedException(
                    String.format(ErrorMessageUtil.ROLE_SCOPE_NOT_ASSIGNABLE, role.getName(), role.getScope()));
        }
        requireSuperAdminForSuperAdminRole(role, grantorRoles);
        if (userId.equals(grantorUserId)) {
            throw new OperationNotPermittedException(ErrorMessageUtil.ROLE_SELF_GRANT_FORBIDDEN);
        }

        User user = getUserOrThrow(userId);
        if (userPlatformRoleRepository.existsByUserIdAndRoleId(userId, roleId)) {
            return;
        }

        requireNotAirlineMember(userId);
        for (UserPlatformRole held : userPlatformRoleRepository.findByUserId(userId)) {
            if (PlatformRoles.conflicts(role.getName(), held.getRole().getName())) {
                throw new ConflictException(String.format(
                        ErrorMessageUtil.ROLE_CONFLICTS_WITH_HELD_ROLE, role.getName(), held.getRole().getName(), userId));
            }
        }

        UserPlatformRole grant = new UserPlatformRole();
        grant.setUser(user);
        grant.setRole(role);
        userPlatformRoleRepository.save(grant);
    }

    @Override
    public void unassignPlatformRoleFromUser(Long roleId, Long userId, Collection<String> grantorRoles) {
        Role role = getRoleById(roleId);
        requireSuperAdminForSuperAdminRole(role, grantorRoles);
        getUserOrThrow(userId);

        UserPlatformRole grant = userPlatformRoleRepository
                .findByUserIdAndRoleId(userId, roleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.PLATFORM_ROLE_NOT_ASSIGNED_TO_USER, roleId, userId)));

        userPlatformRoleRepository.delete(grant);
    }

    private void requireSuperAdminForSuperAdminRole(Role role, Collection<String> grantorRoles) {
        if (PlatformRoles.SUPER_ADMIN.equals(role.getName())
                && !grantorRoles.contains(PlatformRoles.SUPER_ADMIN)) {
            throw new OperationNotPermittedException(String.format(ErrorMessageUtil.ROLE_SUPER_ADMIN_ONLY, role.getName()));
        }
    }

    /** A platform user cannot be an airline member; fails closed (503) when airline-core-service cannot answer. */
    private void requireNotAirlineMember(Long userId) {
        boolean member;
        try {
            member = membershipClient.hasMembership(userId);
        } catch (Exception e) {
            throw new ServiceUnavailableException(ErrorMessageUtil.MEMBERSHIP_CHECK_UNAVAILABLE, e);
        }
        if (member) {
            throw new ConflictException(String.format(ErrorMessageUtil.ROLE_TARGET_IS_AIRLINE_MEMBER, userId));
        }
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.USER_NOT_FOUND_BY_ID, userId)));
    }
}
